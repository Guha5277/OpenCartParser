interface GUIEvents {
    void onUpdaterStart();
    void onUpdaterTotalProducts(int count);
    void onUpdaterProductFailed(String url);
    void onUpdaterCurrentProduct(int position);
    void onUpdaterEnd(int checked, int updated);
}
