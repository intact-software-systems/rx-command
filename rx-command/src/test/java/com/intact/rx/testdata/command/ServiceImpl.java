package com.intact.rx.testdata.command;

public class ServiceImpl implements ServiceInterface {

    @Override
    public Result getResult() {
        return new Result(1, "result");
    }

    @Override
    public Result getResult(String arg1) {
        return new Result(1, arg1);
    }

    @Override
    public Result getResult(String arg1, String arg2) {
        return new Result(1, arg1 + arg2);
    }

    @Override
    public Result getResult(String arg1, String arg2, String arg3) {
        return new Result(1, arg1 + arg2 + arg3);
    }


}
