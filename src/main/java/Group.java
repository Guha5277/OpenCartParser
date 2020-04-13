import java.util.ArrayList;

public class Group {
    private Group parentGroup;
    private ArrayList<Group> childGroups;
    private final String name;
    private final String URL;

    public Group(String name, String URL) {
        this.name = name;
        this.URL = URL;
    }

    public Group(String name, String URL, Group parentGroup) {
        this(name, URL);
        this.parentGroup = parentGroup;
    }

    public void setParentGroup(Group parentGroup) {
        this.parentGroup = parentGroup;
    }

    public void addChild(Group childGroup) {
        if(childGroups == null){
            this.childGroups = new ArrayList<>();
        }
        childGroups.add(childGroup);
    }

    public String getGroupName() {
        return name;
    }

    public String getGroupURL() {
        return URL;
    }

    public Group parent(){
        return parentGroup;
    }

    public Group child(int index){
        return childGroups.get(index);
    }
}
