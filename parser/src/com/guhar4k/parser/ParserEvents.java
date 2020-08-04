package com.guhar4k.parser;

public interface ParserEvents {
    void onGrabberReady();
    void onParserException(Exception e);
    void onUpdaterSQLException(Exception e);
    void onGrabError();
    void onGrabberSuccesfulEnd(int count);

    //updater
    void onUpdateProductFailed(String url, int errorsCount);
    void onUpdaterCurrentProduct(int position, String name);
    void onUpdaterTotalProducts(int count);
    void onUpdateDiffsFound(int count, String differences);
    void onUpdateError();
    void onUpdateSuccessfulEnd(int checked, int updated, int errors);
    void onUpdaterReady();
    void onUpdaterException(int id, String url, Exception e);

    //researcher
    void onResearchSuccessfulEnd(int count);
    void onResearchError();
    void onResearcherReady();
    void onResearcherCurrentCategory(int categoriesCount, int currentCategory, String name);
    void onResearcherCurrentGroup(int groupsCount, int currentGroup, String name);
    void onResearcherFoundNewProduct(String name, int totalInserts);
}
