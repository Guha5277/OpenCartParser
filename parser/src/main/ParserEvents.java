public interface ParserEvents {
    void onParserReady();
    void onUpdaterReady();
    void onResearcherReady();

    void onParserException(Exception e);

    void onParseError();
    void onUpdateError();
    void onResearchError();

    void onParseSuccessfulEnd(int count);
    void onUpdateSuccessfulEnd(int count);
    void onResearchSuccessfulEnd(int count);
}
