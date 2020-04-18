import java.util.ArrayList;

class Category {
    private final String name;
    private final String url;
    private ArrayList<Group> groups;

    Category(String name, String url) {
        this.name = name;
        this.url = url;
    }

    void addGroup(Group group){
        if (groups == null){
            groups = new ArrayList<>();
        }
        groups.add(group);
    }

    String getName() {
        return name;
    }

    String getUrl() {
        return url;
    }

    ArrayList<Group> getGroups() {
        return groups;
    }

}
