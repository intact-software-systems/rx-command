package com.intact.rx.testdata.cache;

public class TopicData {

    private final Integer id;
    private final String data;

    public TopicData(Integer id, String data) {
        this.data = data;
        this.id = id;
    }

    public String getData() {
        return data;
    }

    public Integer getId() {
        return id;
    }

    @SuppressWarnings({"RedundantIfStatement", "EqualsReplaceableByObjectsCall"})
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TopicData topicData = (TopicData) o;

        if (data != null ? !data.equals(topicData.data) : topicData.data != null) {
            return false;
        }
        if (!id.equals(topicData.id)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + (data != null ? data.hashCode() : 0);
        return result;
    }
}
