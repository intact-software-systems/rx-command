package com.intact.rx.testdata.command;

public class Result {

    private final String result;
    private final Integer id;

    public Result(Integer id, String result) {
        this.result = result;
        this.id = id;
    }

    public String getResult() {
        return result;
    }

    public Integer getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Result{" +
                "result='" + result + '\'' +
                ", id=" + id +
                '}';
    }
}
