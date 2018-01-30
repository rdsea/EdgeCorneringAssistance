package model;

/**
 * Created by matthias on 09.10.17.
 */
public class Service {
    private String id;
    private String name;
    private String address;
    private int port;
    private Node node;
    private boolean busy = false;

    public Service() {
    }

    public void setBusy() {
        this.busy = true;
    }

    public boolean isBusy() {
        return this.busy;
    }

    @Override
    public String toString() {
        return "Service{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", port=" + port +
                ", node=" + node +
                '}';
    }

    public Service(String id, String name, String address, int port, Node node) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.port = port;
        this.node = node;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Service service = (Service) o;

        if (port != service.port) return false;
        if (id != null ? !id.equals(service.id) : service.id != null) return false;
        if (name != null ? !name.equals(service.name) : service.name != null) return false;
        if (address != null ? !address.equals(service.address) : service.address != null) return false;
        return node != null ? node.equals(service.node) : service.node == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (address != null ? address.hashCode() : 0);
        result = 31 * result + port;
        result = 31 * result + (node != null ? node.hashCode() : 0);
        return result;
    }

}
