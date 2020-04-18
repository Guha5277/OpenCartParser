import java.util.ArrayList;

class Group {
    private Group parentGroup;
    private ArrayList<Group> childGroups;
    private ArrayList<Liquid> liquids;
    private final String name;
    private final String URL;

    Group(String name, String URL) {
        this.name = name;
        this.URL = URL;
    }

    Group(String name, String URL, Group parentGroup) {
        this(name, URL);
        this.parentGroup = parentGroup;
    }

    void setParentGroup(Group parentGroup) {
        this.parentGroup = parentGroup;
    }

    void addChild(Group childGroup) {
        if(childGroups == null){
            childGroups = new ArrayList<>();
        }
        childGroups.add(childGroup);
    }

    void addLiquid(Liquid liquid){
        if (liquids == null){
            liquids = new ArrayList<>();
        }
        liquids.add(liquid);
    }

    String getGroupName() {
        return name;
    }

    String getGroupURL() {
        return URL;
    }

    ArrayList<Liquid> getLiquids() {
        return liquids;
    }

    Group parent(){
        return parentGroup;
    }

    Group child(int index){
        return childGroups.get(index);
    }

    boolean isGroupEmpty(){
        return (childGroups == null && liquids == null);
    }

}
