package main;

public interface ParserEvents {
    void onGrabberReady();
    void onUpdaterReady();
    void onResearcherReady();

    void onParserException(Exception e);

    void onUpdateProductFailed(String url, int errorsCount);
    void onUpdaterCurrentProduct(int position, String name);
    void onUpdaterTotalProducts(int count);
    void onUpdateDiffsFound(int count);

    void onGrabError();
    void onUpdateError();
    void onResearchError();

    void onParseSuccessfulEnd(int count);
    void onUpdateSuccessfulEnd(int checked, int updated);
    void onResearchSuccessfulEnd(int count);
}
