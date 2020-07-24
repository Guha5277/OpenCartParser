package main;

import com.guhar4k.library.ProductRequest;

class QueryMaker {
    private static final int pageSize = 100;
    private static final int SORT_DEFAULT = 1;
    private static final int SORT_PRICE = 2;
    private static final int SORT_PRICE_DESC = 3;
    private static final int SORT_VOLUME = 4;
    private static final int SORT_VOLUME_DESC = 5;
    private static final int SORT_STRENGTH = 6;
    private static final int SORT_STRENGTH_DESC = 7;

    private int resultsCount;
    private int pages;
    private int currentPage;

    private int sortType;

    private String countQuery;
    private String query;

    QueryMaker(ProductRequest request) {
        make(request);
    }

    //TODO создать метод для установки флага сортировки (есть она или нет)

    String getCountQuery() {
        return countQuery;
    }

    void setSortType(int sortType) {
        this.sortType = sortType;
        currentPage = 0;
    }

    boolean hasNext() {
        return currentPage < pages;
    }

    void makeNewQuery(ProductRequest request) {
        resultsCount = 0;
        pages = 0;
        currentPage = 0;
        sortType = SORT_DEFAULT;
        make(request);
    }

    String getNext() {
        if (!hasNext()) throw new IllegalStateException("The query maker has no next page");
        StringBuilder sb = new StringBuilder();
        sb.append(query);
        sb.append(getSortPart(sortType));
        sb.append(String.format(" LIMIT %d, %d", currentPage * pageSize, pageSize));
        currentPage++;
        return sb.toString();
    }

    String getQuery() {
        currentPage = (currentPage == 0) ? 1 : currentPage;
        return query + getSortPart(sortType) + " LIMIT 100";
    }

    private String getSortPart(int sortType) {
        switch (sortType) {
            case SORT_PRICE:
                return " ORDER BY liquids.price, liquids.our_prod DESC, liquids.category";
            case SORT_PRICE_DESC:
                return " ORDER BY liquids.price DESC, liquids.our_prod DESC, liquids.category";
            case SORT_VOLUME:
                return " ORDER BY liquids.volume, liquids.our_prod DESC, liquids.category";
            case SORT_VOLUME_DESC:
                return " ORDER BY liquids.volume DESC, liquids.our_prod DESC, liquids.category";
            case SORT_STRENGTH:
                return " ORDER BY liquids.strength, liquids.our_prod DESC, liquids.category";
            case SORT_STRENGTH_DESC:
                return " ORDER BY liquids.strength DESC, liquids.our_prod DESC, liquids.category";
            default:
                //TODO дописать
                return " ORDER BY liquids.our_prod DESC, liquids.category";
        }
    }

    void setResultsCount(int resultsCount) {
        this.resultsCount = resultsCount;
        calcPages(resultsCount);
    }

    private void calcPages(int resultsCount) {
        pages = resultsCount / pageSize;
        int remainder = resultsCount % pageSize;
        if (remainder > 0) pages++;
    }

    private void make(ProductRequest request) {
        StringBuilder queryBuilder = new StringBuilder();
        StringBuilder countQueryBuilder = new StringBuilder();
        boolean stockRequired = request.isInStock();

        if (stockRequired) {
            queryBuilder.append(parseStock(countQueryBuilder, request.getRegionID(), request.getStoreID()));
        } else {
            countQueryBuilder.append("SELECT COUNT(id) AS count from liquids");
            queryBuilder.append("SELECT id, name, price, volume, strength, category, url from liquids");
        }

        int strengthStart = request.getStrengthStart();
        int strengthEnd = request.getStrengthEnd();
        int volumeStart = request.getVolumeStart();
        int volumeEnd = request.getVolumeEnd();
        int priceStart = request.getPriceStart();
        int priceEnd = request.getPriceEnd();

        boolean hasWhere = false;

        if (strengthStart > -1 || strengthEnd > -1) {
            String strengthParam = parseParam(hasWhere, "strength", strengthStart, strengthEnd);
            countQueryBuilder.append(strengthParam);
            queryBuilder.append(strengthParam);
            hasWhere = true;
        }

        if (volumeStart > -1 || volumeEnd > -1) {
            String volumeParam = parseParam(hasWhere, "volume", volumeStart, volumeEnd);
            countQueryBuilder.append(volumeParam);
            queryBuilder.append(volumeParam);
            hasWhere = true;
        }

        if (priceStart > -1 || priceEnd > -1) {
            String priceParam = parseParam(hasWhere, "price", priceStart, priceEnd);
            countQueryBuilder.append(priceParam);
            queryBuilder.append(priceParam);
        }

        countQuery = countQueryBuilder.toString();
        query = queryBuilder.toString();

        //return new String[] {countQueryBuilder.toString(), queryBuilder.toString()};
    }

    private String parseStock(StringBuilder distinctQuery, int regionID, int storeID) {
        StringBuilder resultString = new StringBuilder();
        distinctQuery.append("SELECT COUNT (DISTINCT liquids.id) AS count from liquids " +
                "inner join product_remains on product_remains.product_id = liquids.id ");
        resultString.append("SELECT DISTINCT liquids.id, liquids.name, liquids.price, liquids.volume, liquids.strength, liquids.category, liquids.url from liquids " +
                "inner join product_remains on product_remains.product_id = liquids.id ");

        //Stock requirement was checked but request has no selected City(RegionID) or StoreID
        if (regionID == -1 && storeID == -1) {
            String stockAll = "AND product_remains.remains > 0";
            distinctQuery.append(stockAll);
            resultString.append(stockAll);
        }
        //City(RegionID) was selected without select StoreID
        else if (regionID > 0 && storeID == -1) {
            String stockCity = "inner join warehouse on warehouse.id = product_remains.warehouse_id " +
                    "AND product_remains.remains > 0 " +
                    "AND warehouse.region=";
            distinctQuery.append(stockCity);
            distinctQuery.append(regionID);
            resultString.append(stockCity);
            resultString.append(regionID);
        }
        //Warehouse stock (StoreID)
        else {
            String stockCity = "AND product_remains.remains > 0 " +
                    "AND product_remains.warehouse_id=";
            distinctQuery.append(stockCity);
            distinctQuery.append(storeID);
            resultString.append(stockCity);
            resultString.append(storeID);
        }
        return resultString.toString();
    }

    private String parseParam(boolean hasWhere, String paramName, double paramStart, double paramEnd) {
        StringBuilder resultString = new StringBuilder();

        if (hasWhere) {
            resultString.append(" AND ");
        } else {
            resultString.append(" WHERE ");
        }
        resultString.append(paramName);

        if (paramStart > -1 && paramEnd > -1) {
            resultString.append(" BETWEEN ");
            resultString.append(paramStart);
            resultString.append(" AND ");
            resultString.append(paramEnd);
        } else if (paramStart > -1) {
            resultString.append(" >= ");
            resultString.append(paramStart);
        } else {
            resultString.append(" <= ");
            resultString.append(paramEnd);
        }
        return resultString.toString();
    }

    public int getPages() {
        return pages;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof QueryMaker && ((QueryMaker) obj).getQuery().equals(query);
    }
}
