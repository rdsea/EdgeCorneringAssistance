FROM openjdk:8u121-jdk

ADD build/install/recommendation/ /recommendation
ADD demo-config/config.properties /recommendation
WORKDIR /recommendation

CMD ["bin/recommendation-server"]
