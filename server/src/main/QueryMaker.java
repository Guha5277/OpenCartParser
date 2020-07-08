package main;

import com.guhar4k.library.ProductRequest;

class QueryMaker {
    private QueryMaker() {
    }

    static String[] make(ProductRequest request) {
        StringBuilder query = new StringBuilder();
        StringBuilder distinctQuery = new StringBuilder();
        boolean stockRequired = request.isInStock();

        if (stockRequired) {
            query.append(parseStock(distinctQuery, request.getRegionID(), request.getStoreID()));
        } else {
            distinctQuery.append("SELECT COUNT(id) AS count from liquids");
            query.append("SELECT id, name, price, volume, strength, category, url from liquids");
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
            distinctQuery.append(strengthParam);
            query.append(strengthParam);
            hasWhere = true;
        }

        if (volumeStart > -1 || volumeEnd > -1) {
            String volumeParam = parseParam(hasWhere, "volume", volumeStart, volumeEnd);
            distinctQuery.append(volumeParam);
            query.append(volumeParam);
            hasWhere = true;
        }

        if (priceStart > -1 || priceEnd > -1) {
            String priceParam = parseParam(hasWhere, "price", priceStart, priceEnd);
            distinctQuery.append(priceParam);
            query.append(priceParam);
        }

        query.append(" LIMIT 100");

        return new String[] {distinctQuery.toString(), query.toString()};
    }

    private static String parseStock(StringBuilder distinctQuery, int regionID, int storeID) {
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

    private static String parseParam(boolean hasWhere, String paramName, double paramStart, double paramEnd) {
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
