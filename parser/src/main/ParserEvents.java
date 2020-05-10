public interface ParserEvents {
    void onGrabberReady();
    void onUpdaterReady();
    void onResearcherReady();

    void onParserException(Exception e);

    void onUpdateProductFailed(String url);
    void onUpdaterCurrentProduct(int position);
    void onUpdaterTotalProducts(int count);

    void onGrabError();
    void onUpdateError();
    void onResearchError();

    void onParseSuccessfulEnd(int count);
    void onUpdateSuccessfulEnd(int checked, int updated);
    void onResearchSuccessfulEnd(int count);
}
