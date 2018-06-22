package com.intact.rx.testdata.command;

public interface ServiceInterface {

    Result getResult();

    Result getResult(String arg1);

    Result getResult(String arg1, String arg2);

    Result getResult(String arg1, String arg2, String arg3);
}
