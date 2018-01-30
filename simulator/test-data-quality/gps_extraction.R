# Create a GPS track from an OSRM-Response.json file

# Convert json to csv
library(jsonlite)
library(geosphere)
library(dplyr)

json <- fromJSON(txt = "osrm-response.json", simplifyDataFrame = TRUE)
legs <- json$routes$legs
df <- legs[[1]]
ann <- df$annotation

overpassRequest <- "http://www.overpass-api.de/api/interpreter?data=[out:json];"
for (index in 1:nrow(ann)) {
  nodeList <- ann[index,1]
  nodeIds <- as.numeric(unlist(nodeList))
  for (idx in 1:length(nodeIds)) {
    overpassRequest <- paste0(overpassRequest,"node(",nodeIds[idx],");out;")
  }
}

json <- fromJSON(overpassRequest, simplifyDataFrame = TRUE)
elements <- json$elements
elements <- elements[,c("lat", "lon")]
track <- elements
track <- track[,1:2]
track <- unique(track)
distM <- distm(x =track, fun = distHaversine)  
track$dist <- 0
for(index in 2:nrow(distM)) {
  track[index, "dist"] <- distM[index, index-1]
}
write.csv(file="tracks/thesis-test-track.csv", x = track, row.names = FALSE)