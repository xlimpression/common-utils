package com.common.tool.html;

import java.util.List;

public class Table {
    private List<String> content;
    private List<String> preContent;

    public Table() {
    }

    public List<String> getContent() {
        return this.content;
    }

    public void setContent(List<String> content) {
        this.content = content;
    }

    public List<String> getPreContent() {
        return this.preContent;
    }

    public void setPreContent(List<String> preContent) {
        this.preContent = preContent;
    }
}