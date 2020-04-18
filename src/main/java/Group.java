import java.util.ArrayList;

public class Group {
    private Group parentGroup;
    private ArrayList<Group> childGroups;
    private ArrayList<Liquid> liquids;
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
            childGroups = new ArrayList<>();
        }
        childGroups.add(childGroup);
    }

    public void addLiquid(Liquid liquid){
        if (liquids == null){
            liquids = new ArrayList<>();
        }
        liquids.add(liquid);
    }

    public String getGroupName() {
        return name;
    }

    public String getGroupURL() {
        return URL;
    }

    public ArrayList<Liquid> getLiquids() {
        return liquids;
    }

    public Group parent(){
        return parentGroup;
    }

    public Group child(int index){
        return childGroups.get(index);
    }

    public boolean isGroupEmpty(){
        return (childGroups == null && liquids == null);
    }

}
