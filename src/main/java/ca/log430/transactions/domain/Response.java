package ca.log430.transactions.domain;

public class Response<T> {

    private T data;
    private String error;

    public Response(T data, String error) {
        this.data = data;
        this.error = error;
    }
    public T getData() {
        return data;
    }
    public String getError() {
        return error;
    }
    public void setData(T data) {
        this.data = data;
    }

    public void setError(String error) {
        this.error = error;
    }
}
