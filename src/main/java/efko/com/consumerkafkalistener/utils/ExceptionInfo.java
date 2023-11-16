package efko.com.consumerkafkalistener.utils;


import java.util.ArrayList;
import java.util.List;

public class ExceptionInfo {
    private String file;
    private String line;
    private int code;
    private List<String> trace;

    public ExceptionInfo() {
        this.trace = new ArrayList<>();
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public List<String> getTrace() {
        return trace;
    }

    public void setTrace(List<String> trace) {
        this.trace = trace;
    }
}