package com.aws.vokunev.catalog.data;

import java.util.ArrayList;
import java.util.Iterator;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;

public class ProductCatalogAccessor {

    static AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().withRegion(Regions.US_WEST_2).build();
    static DynamoDB dynamoDB = new DynamoDB(client);
    static String tableName = "ProductCatalog";

    public static ArrayList<CatalogItem> getProductCatalog() {

        ArrayList<CatalogItem> catalog = new ArrayList<CatalogItem>();

        Table table = dynamoDB.getTable(tableName);
        
        // Fetch the records form DynamoDB table
        ItemCollection<ScanOutcome> items = table.scan();

        // Populate Product Catalog list from the database records
        Iterator<Item> iterator = items.iterator();
        while (iterator.hasNext()) {

            CatalogItem item = new CatalogItem();

            Item record = iterator.next();

            item.setId(record.getInt("Id"));
            item.setTitle(record.getString("Title"));
            item.setDescription(record.getString("Description"));
            item.setProductCategory(record.getString("ProductCategory"));
            item.setYear(record.getInt("Year"));
            item.setImage(record.getString("Image"));
            item.setPrice(record.getFloat("Price"));
            catalog.add(item);

            // Log the created item
            System.out.println(item);
        }

        return catalog;
    }    
}