package com.guhar4k.parser;

public interface IResearcher extends IParser{
    void onResearchSuccessfulEnd(int count);
    void onResearchError();
    void onResearcherReady();
    void onResearcherCurrentCategory(int categoriesCount, int currentCategory, String name);
    void onResearcherCurrentGroup(int groupsCount, int currentGroup, String name);
    void onResearcherFoundNewProduct(String name, int totalInserts);
    void onResearcherException(String message);

    boolean isProductAlreadyInDB(String url);
}
