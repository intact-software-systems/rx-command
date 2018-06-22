package com.intact.rx.exception;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.stream.Stream;

import com.intact.rx.api.FutureStatus;
import com.intact.rx.api.command.Action;

public final class ExceptionMessageFactory {

    public static String buildExceptionsMessage(Map<Action, Throwable> exceptions) {
        StringBuilder builder = new StringBuilder();
        exceptions.forEach(
                (action, throwable) -> {
                    builder.append("\t\t\taction=")
                            .append(action)
                            .append("\n\t\t\tthrowable=\n");
                    appendThrowable(builder, throwable)
                            .append("\n\t\t\t");
                }
        );
        return builder.toString();
    }

    private static StringBuilder appendThrowable(StringBuilder sb, Throwable t) {
        try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
            t.printStackTrace(pw);
            String str = sw.toString();
            Stream.of(str.split("\n")).forEach(l ->
                    sb.append("\t\t\t\t").append(l).append("\n"));
        } catch (IOException e) {
            sb.append("Failed to write ").append(t).append(": ").append(e);
        }
        return sb;
    }

    public static <V> V throwFutureException(FutureStatus status, Object key, Throwable throwable) {
        switch (status) {
            case NotStarted:
                throw new ExecutionNotStartedException("Request of key " + key + " is not started", throwable);
            case Success:
                throw new ExecutionEndedWithNoResultException("Request of key " + key + " succeeded, but no result cached", throwable);
            case Failed:
                throw new ExecutionEndedOnErrorException("Request of key " + key + " failed", throwable);
            case Subscribed:
            case Timedout:
                throw new WaitForExecutionTimedOutException("Request of key " + key + " timed out", throwable);
        }

        throw new ExecutionEndedOnErrorException("Request of key " + key + " failed", throwable);
    }

    private ExceptionMessageFactory() {
    }
}
