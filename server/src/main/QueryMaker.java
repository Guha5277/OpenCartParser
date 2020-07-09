package main;

import com.guhar4k.library.ProductRequest;
import org.omg.CosNaming.NamingContextExtPackage.StringNameHelper;

class QueryMaker {
    private static final int pageSize = 100;
    private int resultsCount;
    private int pages;
    private int currentPage;

    private String countQuery;
    private String query;

    QueryMaker(ProductRequest request) {
        make(request);
    }

    String getCountQuery() {
        return countQuery;
    }

    boolean hasNext(){
        return currentPage * pageSize < resultsCount;
    }

    String getNext(){
        if (!hasNext()) throw new IllegalStateException("The query maker has no next page");
        currentPage++;
        String limit = String.format(" LIMIT %d, %d", currentPage * pageSize, pageSize);
        return query + limit;
    }

    String getQuery() {
        currentPage = (currentPage == 0) ? 1 : currentPage;
        return query + " LIMIT 100";
    }

    void setResultsCount(int resultsCount) {
        this.resultsCount = resultsCount;
        calcPages(resultsCount);
    }

    private void calcPages(int resultsCount){
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
            resultString.append(" > ");
            resultString.append(paramStart);
        } else {
            resultString.append(" < ");
            resultString.append(paramEnd);
        }
        return resultString.toString();
    }
}
