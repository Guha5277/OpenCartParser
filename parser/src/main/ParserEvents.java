public interface ParserEvents {
    void onParseStarted();
    void onParserException(Exception e);
    void onParseError();
    void onParseSuccessfulEnd(int count);
    void onUpdateSuccessfulEnd(int count);
}
