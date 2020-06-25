package functions.vo;

public class FileVO {
    private String url;
    private String objectid;

    public FileVO(String url, String objectid) {
        this.url = url;
        this.objectid = objectid;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getObjectid() {
        return objectid;
    }

    public void setObjectid(String objectid) {
        this.objectid = objectid;
    }

    public String toString() {
        String output = String.format("{ \"url\": \"%s\", \"objectid\": \"%s\" }", url, objectid);
        System.out.println("output:"+output);
        //        return "{\"url\": \""+url+"\", \"objectid\": \""+objectid+"\" }";
        return output;
    }
}
