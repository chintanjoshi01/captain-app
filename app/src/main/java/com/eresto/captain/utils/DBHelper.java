package com.eresto.captain.utils;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


import com.eresto.captain.model.CartItemRow;
import com.eresto.captain.model.GetTables;
import com.eresto.captain.model.Item;
import com.eresto.captain.model.MenuData;
import com.eresto.captain.model.PriceTemplateData;
import com.eresto.captain.model.PrinterRespo;
import com.eresto.captain.model.PrinterTypes;
import com.eresto.captain.model.kitCat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressLint("Range")
public class DBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "captain_eresto.db";
    public static final String TABLE_PRINTER = "printers";
    public static final String MENU_SYNC_CATEGORY_TABLE_NAME = "menu_sync_category";
    public static final String MENU_SYNC_ITEM_TABLE_NAME = "menu_sync_item";
    public static final String TABLE_TABLE_NAME = "table_master";
    public static final String TABLE_ITEM = "cart_item";

    public static final String TABLE_KIT_CAT = "kit_cat";
    public static final String TABLE_PRICE_TEMPLATE = "resto_price_template";
    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "create table " + TABLE_KIT_CAT +
                        "(" +
                        "id integer," +
                        "kit_cat text)"
        );

        db.execSQL(
                "create table " + TABLE_PRINTER +
                        "(" +
                        "id integer," +
                        "name text," +
                        "connection integer," +
                        "print integer," +
                        "ip text," +
                        "port_add text," +
                        "port text," +
                        "indexs text)"
        );

        db.execSQL(
                "create table " + TABLE_TABLE_NAME +
                        "(" +
                        "id integer," +
                        "tab_label text," +
                        "status integer," +
                        "tab_type integer)"
        );

        db.execSQL(
                "create table " + MENU_SYNC_CATEGORY_TABLE_NAME +
                        "(id integer PRIMARY KEY AUTOINCREMENT, category_name text,category_display_order integer,price_temp_id integer,item_cat_id integer,en integer,menu_type text)"
        );
        db.execSQL(
                "create table " + MENU_SYNC_ITEM_TABLE_NAME +
                        "(" +
                        "local_id integer PRIMARY KEY AUTOINCREMENT," +
                        "id integer," +
                        "item_name text," +
                        "item_price String," +
                        "item_cat_id integer," +
                        "menu_type integer," +
                        "kitchen_cat_id integer," +
                        "item_is_nonveg integer," +
                        "sp_inst text," +
                        "item_tax text)"
        );
        db.execSQL(
                "create table " + TABLE_ITEM +
                        "(" +
                        "id integer PRIMARY KEY AUTOINCREMENT," +
                        "item_id integer," +
                        "kot_id integer," +
                        "item_name text," +
                        "item_short_name text," +
                        "item_price String," +
                        "sp_inst text," +
                        "item_cat_id integer," +
                        "table_id integer," +
                        "pre_order_id text," +
                        "qty integer," +
                        "ncv integer," +
                        "notes text," +
                        "kitchen_cat_id integer," +
                        "item_tax_json text," +
                        "item_tax_amt text," +
                        "item_amt text," +
                        "sorting integer)"
        );
        db.execSQL(
                "create table " + TABLE_PRICE_TEMPLATE +
                        "(temp_id integer,temp_name text,menu_type integer)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRINTER);

        db.execSQL("DROP TABLE IF EXISTS " + MENU_SYNC_CATEGORY_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + MENU_SYNC_ITEM_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TABLE_NAME);

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRICE_TEMPLATE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ITEM);
        db.delete(TABLE_KIT_CAT, null, null);
        onCreate(db);
    }

    public boolean DeleteDB() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PRINTER, null, null);
        db.delete(MENU_SYNC_CATEGORY_TABLE_NAME, null, null);
        db.delete(MENU_SYNC_ITEM_TABLE_NAME, null, null);
        db.delete(TABLE_TABLE_NAME, null, null);

        db.delete(TABLE_PRICE_TEMPLATE, null, null);
        db.delete(TABLE_ITEM, null, null);
        db.delete(TABLE_KIT_CAT, null, null);
        return true;
    }
    public boolean InsertKitCat(List<kitCat> list) {

        if (list.size() > 0) {
            deleteAllKitCat();
            SQLiteDatabase db = this.getWritableDatabase();
            String query = "INSERT INTO " + TABLE_KIT_CAT + " ( id,kit_cat) VALUES";
            for (int i = 0; i < list.size(); i++) {
                query += (" (" + list.get(i).getId() + ",'"
                        + list.get(i).getItem_kitchen_cat() + "'),");
            }
            query = query.substring(0, query.length() - 1) + ";";
            Log.d("sqllite", query);
            db.execSQL(query);
        }
        return true;
    }

    public ArrayList<kitCat> GetKitCat() {
        ArrayList<kitCat> array_list = new ArrayList<kitCat>();


        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from " + TABLE_KIT_CAT,
                null);
        res.moveToFirst();

        while (!res.isAfterLast()) {
            array_list.add(new kitCat(
                            res.getInt(res.getColumnIndex("id")),
                            res.getString(res.getColumnIndex("kit_cat"))
                    )
            );
            res.moveToNext();
        }
        return array_list;
    }

    public kitCat GetKitCatSingle(int id) {
        kitCat array_list = null;


        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from " + TABLE_KIT_CAT + " where id=" + id,
                null);
        res.moveToFirst();
        if (res.getCount() > 0) {
            array_list = new kitCat(
                    res.getInt(res.getColumnIndex("id")),
                    res.getString(res.getColumnIndex("kit_cat"))
            );
        }
        return array_list;
    }

    public boolean InsertTableItems(CartItemRow list) {
        //deleteArea();
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "INSERT INTO " + TABLE_ITEM +
                " ( item_id,item_name,item_short_name,item_price,sp_inst," +
                "item_cat_id,table_id,pre_order_id,qty,ncv,notes,kitchen_cat_id," +
                "item_tax_json,item_tax_amt,item_amt,sorting)" +
                " VALUES";

        query += (" (" +
                list.getId() + ",'"
                + replaceWithAphostrophy(list.getItem_name()) + "','"
                + replaceWithAphostrophy(list.getItem_short_name()) + "','"
                + list.getItem_price() + "','"
                + replaceWithAphostrophy(list.getSp_inst()) + "',"
                + list.getItem_cat_id() + ","
                + list.getTable_id() + ","
                + list.getPre_order_id() + ","
                + list.getQty() + ","
                + list.getKot_ncv() + ",'"
                + list.getNotes() + "',"
                + list.getKitchen_cat_id() + ",'"
                + list.getItem_tax() + "','"
                + list.getItem_tax_amt() + "','"
                + list.getItem_amt() + "',"
                + list.getSorting() + ""
                + ")");
        db.execSQL(query);
        return true;
    }


    public boolean InsertMenuSyncCategory(MenuData list, int priceTemp) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "INSERT INTO " + MENU_SYNC_CATEGORY_TABLE_NAME +
                " ( category_name, category_display_order, item_cat_id,en,price_temp_id,menu_type) VALUES";

        query += (" ('" + list.getCategory_name().replace("'", "''") + "',"
                + list.getCategory_display_order() + ","
                + list.getItem_cat_id() + ","
                + list.getEn() + ","
                + priceTemp + ",'"
                + list.getMenu_type() + "'),");
        query = query.substring(0, query.length() - 1) + ";";
        db.execSQL(query);
        InsertSyncMenuItems(list.getItems());
        return true;
    }


    public boolean InsertSyncMenuItems(List<Item> list) {
//        deleteAllSyncItems();
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "INSERT INTO " + MENU_SYNC_ITEM_TABLE_NAME +
                " ( id, " +
                "item_name, " +
                "item_price, " +
                "sp_inst," +
                "item_cat_id, " +
                "kitchen_cat_id," +
                "menu_type," +
                "item_is_nonveg," +
                "item_tax)" +
                " VALUES";
        for (int i = 0; i < list.size(); i++) {
            query += (" ("
                    + list.get(i).getItem_id() + ",\""
                    + replaceWithAphostrophy(list.get(i).getItem_name()) + "\",'"
                    + list.get(i).getItem_price() + "','"
                    + replaceWithAphostrophy(list.get(i).getSp_inst()) + "',"
                    + list.get(i).getItem_cat_id() + ","
                    + list.get(i).getKitchen_cat_id() + ","
                    + list.get(i).getMenu_type() + ","
                    + list.get(i).is_nonveg() + ",'"
                    + list.get(i).getItem_tax() + "'),");
        }
        query = query.substring(0, query.length() - 1) + ";";
        db.execSQL(query);
        return true;
    }

    public String replaceWithAphostrophy(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        } else {
            return text.replace("'", "`");
        }
    }

    public boolean InsertPrinter(List<PrinterRespo> list) {
        deleteAllPrinter();
        SQLiteDatabase db = this.getWritableDatabase();

        String query = "INSERT INTO " + TABLE_PRINTER + " ( id,name,connection,print,ip,port_add,port,indexs) VALUES";
        for (int i = 0; i < list.size(); i++) {
                query += (" ("
                        + list.get(i).getId() + ",'"
                        + list.get(i).getPrinter_name() + "',"
                        + list.get(i).getPrinter_connection_type_id() + ","
                        + list.get(i).getPrinter_type() + ",'"
                        + list.get(i).getIp_add() + "','"
                        + list.get(i).getPrinter_port() + "','"
                        + list.get(i).getPort_add() + "','"
                        + list.get(i).getIndex() + "'),");

        }
        query = query.substring(0, query.length() - 1) + ";";
        Log.d("sqllite", query);
        db.execSQL(query);
        return true;
    }

    public boolean InsertTable(List<GetTables> list) {
        deleteAllTables();
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "INSERT INTO " + TABLE_TABLE_NAME +
                " ( id, status, tab_label,  tab_type) VALUES";
        for (int i = 0; i < list.size(); i++) {
            String tabel = list.get(i).getTab_label() == null ? "-" : list.get(i).getTab_label();
            query += (" (" + list.get(i).getId() + ","
                    + list.get(i).getTab_status() + ",'"
                    + tabel + "',"
                    + list.get(i).getTab_type() + "),");
        }
        query = query.substring(0, query.length() - 1) + ";";
        db.execSQL(query);
        return true;
    }
    public int UpdateTable(int status, String table) {
        int count = 0;
        try (SQLiteDatabase db = this.getWritableDatabase()) { // ✅ Auto-close db after use
            if (table.isEmpty()) {
                ContentValues cv = new ContentValues();
                cv.put("status", status);
                count = db.update(TABLE_TABLE_NAME, cv, null, null);
            } else {
                String sql = "UPDATE " + TABLE_TABLE_NAME + " SET status = ? WHERE id IN (" + table + ")";
                db.execSQL(sql, new Object[]{status}); // ✅ Use parameterized query
                count = 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }


    public int UpdateTableItem(CartItemRow list) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("qty", list.getQty());
        cv.put("item_price", list.getItem_price());
        cv.put("notes", (list.getNotes()));
        cv.put("kitchen_cat_id", (list.getKitchen_cat_id()));
        cv.put("ncv", (list.getKot_ncv()));
        cv.put("item_tax_json", (list.getItem_tax()));
        cv.put("item_tax_amt", (list.getItem_tax_amt()));
        cv.put("item_amt", (list.getItem_amt()));

        return db.update(TABLE_ITEM, cv, "item_id = ? AND table_id = ?",
                new String[]{String.valueOf(list.getId()), String.valueOf(list.getTable_id())});
    }
    public int UpdateTableItemQty(CartItemRow list) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("qty", list.getQty()); // Only updating qty

        return db.update(TABLE_ITEM, cv, "item_id = ? AND table_id = ?",
                new String[]{String.valueOf(list.getId()), String.valueOf(list.getTable_id())});
    }

    public int UpdateTableItemsNcv(List<CartItemRow> items, int ncv) {
        SQLiteDatabase db = this.getWritableDatabase();
        for (CartItemRow list : items) {
            ContentValues cv = new ContentValues();
            cv.put("ncv", ncv); // Only updating ncv

            db.update(TABLE_ITEM, cv, "item_id = ? AND table_id = ?",
                    new String[]{String.valueOf(list.getId()), String.valueOf(list.getTable_id())});
        }
        return 1;
    }



    public int UpdateTableItems(List<CartItemRow> items) {
        SQLiteDatabase db = this.getWritableDatabase();
        for (int i = 0; i < items.size(); i++) {
            CartItemRow list = items.get(i);
            ContentValues cv = new ContentValues();
            cv.put("qty", list.getQty());
            cv.put("item_price", list.getItem_price());
            cv.put("notes", (list.getNotes()));
            cv.put("kitchen_cat_id", (list.getKitchen_cat_id()));
            cv.put("ncv", (list.getKot_ncv()));
            cv.put("item_tax_json", (list.getItem_tax()));
            cv.put("item_tax_amt", (list.getItem_tax_amt()));
            cv.put("item_amt", (list.getItem_amt()));
            db.update(TABLE_ITEM, cv, "item_id = ? AND table_id = ?",
                    new String[]{String.valueOf(list.getId()), String.valueOf(list.getTable_id())});

        }
        return 1;
    }


    public boolean InsertPriceTemplate(List<PriceTemplateData> list) {
        deleteAllPriceTemplate();
        SQLiteDatabase db = this.getWritableDatabase();
        StringBuilder query = new StringBuilder("INSERT INTO " + TABLE_PRICE_TEMPLATE +
                " ( temp_id, temp_name,menu_type) VALUES");
        for (int i = 0; i < list.size(); i++) {
            query.append(" (").append(list.get(i).getPt_id()).append(",'").append(list.get(i).getPrice_template()).append("',").append(list.get(i).getMenu_type()).append("),");
        }
        query = new StringBuilder(query.substring(0, query.length() - 1) + ";");
        db.execSQL(query.toString());
        return true;
    }


    public ArrayList<PriceTemplateData> GetPriceTemplate() {
        ArrayList<PriceTemplateData> array_list = new ArrayList<>();

        //hp = new HashMap();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from " + TABLE_PRICE_TEMPLATE, null);
        res.moveToFirst();

        while (!res.isAfterLast()) {
            array_list.add(new PriceTemplateData(
                    res.getInt(res.getColumnIndex("temp_id")),
                    res.getString(res.getColumnIndex("temp_name")),
                    null,
                    res.getInt(res.getColumnIndex("menu_type")),
                    false)
            );
            res.moveToNext();
        }
        return array_list;
    }
    public List<CartItemRow> GetCartItems(int tableId) {
        ArrayList<CartItemRow> array_list = new ArrayList<CartItemRow>();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from " + TABLE_ITEM + " where table_id = " + tableId + " ORDER BY id ASC, sorting",
                null);
        res.moveToFirst();

        while (!res.isAfterLast()) {
            array_list.add(new CartItemRow(
                    res.getInt(res.getColumnIndex("item_id")),
                    res.getInt(res.getColumnIndex("kot_id")),
                    res.getString(res.getColumnIndex("item_name")),
                    res.getString(res.getColumnIndex("item_short_name")),
                    Double.parseDouble(res.getString(res.getColumnIndex("item_price"))),
                    res.getString(res.getColumnIndex("sp_inst")),
                    res.getInt(res.getColumnIndex("item_cat_id")),
                    res.getInt(res.getColumnIndex("table_id")),
                    res.getString(res.getColumnIndex("pre_order_id")),
                    res.getInt(res.getColumnIndex("qty")),
                    res.getInt(res.getColumnIndex("ncv")),
                    res.getString(res.getColumnIndex("notes")),
                    res.getInt(res.getColumnIndex("kitchen_cat_id")),
                    res.getString(res.getColumnIndex("item_tax_json")),
                    res.getString(res.getColumnIndex("item_tax_amt")),
                    res.getString(res.getColumnIndex("item_amt")),
                    res.getInt(res.getColumnIndex("sorting"))));
            res.moveToNext();
        }
        return array_list;
    }
    public CartItemRow GetCartItems(int tableId, int itemId) {

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from " + TABLE_ITEM + "" +
                        " where table_id = " + tableId + " AND item_id=" + itemId,
                null);
        if (res.moveToFirst()) {
            return new CartItemRow(
                    res.getInt(res.getColumnIndex("item_id")),
                    res.getInt(res.getColumnIndex("kot_id")),
                    res.getString(res.getColumnIndex("item_name")),
                    res.getString(res.getColumnIndex("item_short_name")),
                    Double.parseDouble(res.getString(res.getColumnIndex("item_price"))),
                    res.getString(res.getColumnIndex("sp_inst")),
                    res.getInt(res.getColumnIndex("item_cat_id")),
                    res.getInt(res.getColumnIndex("table_id")),
                    res.getString(res.getColumnIndex("pre_order_id")),
                    res.getInt(res.getColumnIndex("qty")),
                    res.getInt(res.getColumnIndex("ncv")),
                    res.getString(res.getColumnIndex("notes")),
                    res.getInt(res.getColumnIndex("kitchen_cat_id")),
                    res.getString(res.getColumnIndex("item_tax_json")),
                    res.getString(res.getColumnIndex("item_tax_amt")),
                    res.getString(res.getColumnIndex("item_amt")),
                    res.getInt(res.getColumnIndex("sorting"))
            );
        } else {
            return null;
        }
    }

    public ArrayList<MenuData> GetSyncItems(int page) {
        ArrayList<Item> array_list = new ArrayList<Item>();
        int offset = 50 * page;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from " + MENU_SYNC_ITEM_TABLE_NAME + " ORDER BY local_id ASC LIMIT 50 OFFSET " + offset,
                null);
        res.moveToFirst();
        while (!res.isAfterLast()) {
            array_list.add(new Item(
                            0,
                            res.getInt(res.getColumnIndex("id")),
                            res.getInt(res.getColumnIndex("id")), 0, 0, 0, 0,
                            res.getInt(res.getColumnIndex("item_cat_id")),
                            "",
                            1,
                            res.getInt(res.getColumnIndex("item_is_nonveg")),
                            0, 0,
                            res.getString(res.getColumnIndex("item_name")), "",
                             Double.parseDouble(res.getString(res.getColumnIndex("item_price"))), "",
                            "", "", "",
                            res.getString(res.getColumnIndex("sp_inst")),
                            "",
                            "",
                            "",
                            res.getInt(res.getColumnIndex("kitchen_cat_id")), 0,
                            "",
                            false,
                            0,
                            "", false, 0, 1,
                            res.getInt(res.getColumnIndex("menu_type")),
                            0, "", 0, res.getString(res.getColumnIndex("item_tax"))
                    )
            );
            res.moveToNext();
        }
        String catId = "";
        for (int i = 0; i < array_list.size(); i++) {
            int found = 0;
            String[] catIds = catId.split(",");
            for (int j = 0; j < catIds.length; j++) {
                String cat = catIds[j];
                if (!cat.isEmpty()) {
                    if (array_list.get(i).getItem_cat_id() == Integer.parseInt(cat)) {
                        found = 1;
                    }
                }
            }
            if (found == 0) {
                catId += array_list.get(i).getItem_cat_id() + ",";
            }
        }
        if (catId.length() > 1)
            catId = catId.substring(0, catId.length() - 1);
        ArrayList<MenuData> menuCat = new ArrayList<MenuData>();
        Cursor resCat = db.rawQuery("select * from " + MENU_SYNC_CATEGORY_TABLE_NAME + " WHERE item_cat_id IN (" + catId + ") ORDER BY id ASC", null);
        resCat.moveToFirst();

        while (!resCat.isAfterLast()) {
            int itemCatId = resCat.getInt(resCat.getColumnIndex("item_cat_id"));
            String menuType = resCat.getString(resCat.getColumnIndex("menu_type"));
            int mt = 1;
            if (menuType != null && !menuType.equals("null")) mt = Integer.parseInt(menuType);
            menuCat.add(new MenuData(
                    resCat.getInt(resCat.getColumnIndex("category_display_order")),
                    resCat.getString(resCat.getColumnIndex("category_name")),
                    itemCatId,
                    resCat.getInt(resCat.getColumnIndex("price_temp_id")),
                    resCat.getInt(resCat.getColumnIndex("en")),
                    mt, false, false,
                    new ArrayList<>()
            ));
            resCat.moveToNext();
        }

        for (int j = 0; j < menuCat.size(); j++) {
            int itemCatId = menuCat.get(j).getItem_cat_id();
            ArrayList<Item> arrayListCat = new ArrayList<Item>();
            for (int i = 0; i < array_list.size(); i++) {
                if (array_list.get(i).getItem_cat_id() == itemCatId) {
                    arrayListCat.add(array_list.get(i));
                }
            }
            menuCat.get(j).setItems(arrayListCat);
        }
        return menuCat;
    }

    public ArrayList<MenuData> GetSyncCategories() {

        ArrayList<MenuData> menuCat = new ArrayList<MenuData>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor resCat = db.rawQuery("select * from " + MENU_SYNC_CATEGORY_TABLE_NAME, null);
        resCat.moveToFirst();

        while (!resCat.isAfterLast()) {
            int itemCatId = resCat.getInt(resCat.getColumnIndex("item_cat_id"));
            String menuType = resCat.getString(resCat.getColumnIndex("menu_type"));
            int mt = 1;
            if (menuType != null && !menuType.equals("null")) mt = Integer.parseInt(menuType);
            menuCat.add(new MenuData(
                    resCat.getInt(resCat.getColumnIndex("category_display_order")),
                    resCat.getString(resCat.getColumnIndex("category_name")),
                    itemCatId,
                    resCat.getInt(resCat.getColumnIndex("price_temp_id")),
                    resCat.getInt(resCat.getColumnIndex("en")),
                    mt, false, false,
                    new ArrayList<>()
            ));
            resCat.moveToNext();
        }
        return menuCat;
    }

    public ArrayList<MenuData> GetSyncItemsByCat(int cat_Id) {
        ArrayList<Item> array_list = new ArrayList<Item>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from " + MENU_SYNC_ITEM_TABLE_NAME + " WHERE item_cat_id=" + cat_Id,
                null);
        res.moveToFirst();
        while (!res.isAfterLast()) {
            array_list.add(new Item(
                            0,
                            res.getInt(res.getColumnIndex("id")),
                            res.getInt(res.getColumnIndex("id")), 0, 0, 0, 0,
                            res.getInt(res.getColumnIndex("item_cat_id")),
                            "",
                            1,
                            res.getInt(res.getColumnIndex("item_is_nonveg")),
                            0, 0,
                            res.getString(res.getColumnIndex("item_name")), "",

                    Double.parseDouble(res.getString(res.getColumnIndex("item_price"))),"",
                            "", "", "",
                            res.getString(res.getColumnIndex("sp_inst")),
                            "",
                            "",
                            "",
                            res.getInt(res.getColumnIndex("kitchen_cat_id")), 0,
                            "",
                            false,
                            0,
                            "", false, 0, 1,
                            res.getInt(res.getColumnIndex("menu_type")),
                            0, "", 0, res.getString(res.getColumnIndex("item_tax"))
                    )
            );
            res.moveToNext();
        }
        String catId = "";
        for (int i = 0; i < array_list.size(); i++) {
            int found = 0;
            String[] catIds = catId.split(",");
            for (int j = 0; j < catIds.length; j++) {
                String cat = catIds[j];
                if (!cat.isEmpty()) {
                    if (array_list.get(i).getItem_cat_id() == Integer.parseInt(cat)) {
                        found = 1;
                    }
                }
            }
            if (found == 0) {
//                catId.add(array_list.get(i).getItem_cat_id());
                catId += array_list.get(i).getItem_cat_id() + ",";
            }
        }
        if (catId.length() > 1)
            catId = catId.substring(0, catId.length() - 1);
        ArrayList<MenuData> menuCat = new ArrayList<MenuData>();
        Cursor resCat = db.rawQuery("select * from " + MENU_SYNC_CATEGORY_TABLE_NAME + " WHERE item_cat_id IN (" + catId + ") ORDER BY id ASC", null);
        resCat.moveToFirst();

        while (!resCat.isAfterLast()) {
            int itemCatId = resCat.getInt(resCat.getColumnIndex("item_cat_id"));
            String menuType = resCat.getString(resCat.getColumnIndex("menu_type"));
            int mt = 1;
            if (menuType != null && !menuType.equals("null")) mt = Integer.parseInt(menuType);
            menuCat.add(new MenuData(
                    resCat.getInt(resCat.getColumnIndex("category_display_order")),
                    resCat.getString(resCat.getColumnIndex("category_name")),
                    itemCatId,
                    resCat.getInt(resCat.getColumnIndex("price_temp_id")),
                    resCat.getInt(resCat.getColumnIndex("en")),
                    mt, false, false,
                    new ArrayList<>()
            ));
            resCat.moveToNext();
        }

        for (int j = 0; j < menuCat.size(); j++) {
            int itemCatId = menuCat.get(j).getItem_cat_id();
            ArrayList<Item> arrayListCat = new ArrayList<Item>();
            for (int i = 0; i < array_list.size(); i++) {
                if (array_list.get(i).getItem_cat_id() == itemCatId) {
                    arrayListCat.add(array_list.get(i));
                }
            }
            menuCat.get(j).setItems(arrayListCat);
        }
        return menuCat;
    }

    public ArrayList<Item> GetSyncSearchItems(String q) {
        ArrayList<Item> array_list = new ArrayList<Item>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from " + MENU_SYNC_ITEM_TABLE_NAME + " WHERE item_name LIKE '%" + q + "%'" + " AND item_cat_id <> -1 ORDER BY local_id ASC ",
                null);
        res.moveToFirst();
        while (!res.isAfterLast()) {
            array_list.add(new Item(
                            0,
                            res.getInt(res.getColumnIndex("id")),
                            res.getInt(res.getColumnIndex("id")), 0, 0, 0, 0,
                            res.getInt(res.getColumnIndex("item_cat_id")),
                            "",
                            1,
                            res.getInt(res.getColumnIndex("item_is_nonveg")),
                            0, 0,
                            res.getString(res.getColumnIndex("item_name")), "",

                    Double.parseDouble(res.getString(res.getColumnIndex("item_price"))), "",
                            "", "", "",
                            res.getString(res.getColumnIndex("sp_inst")),
                            "",
                            "",
                            "",
                            res.getInt(res.getColumnIndex("kitchen_cat_id")), 0,
                            "",
                            false,
                            0,
                            "", false, 0, 1,
                            res.getInt(res.getColumnIndex("menu_type")),
                            0, "", 0, res.getString(res.getColumnIndex("item_tax"))
                    )
            );
            res.moveToNext();
        }


        return array_list;
    }


    public ArrayList<PrinterRespo> GetPrinters() {
        ArrayList<PrinterRespo> array_list = new ArrayList<PrinterRespo>();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from " + TABLE_PRINTER, null);
        res.moveToFirst();

        while (!res.isAfterLast()) {
            array_list.add(new PrinterRespo(
                    res.getInt(res.getColumnIndex("id")),
                    res.getString(res.getColumnIndex("name")),
                    res.getInt(res.getColumnIndex("connection")),
                    res.getInt(res.getColumnIndex("print")),
                    res.getString(res.getColumnIndex("ip")),
                    res.getString(res.getColumnIndex("port")),
                    res.getString(res.getColumnIndex("port_add")),
                    res.getString(res.getColumnIndex("indexs"))
            ));
            res.moveToNext();
        }
        return array_list;
    }

    public PrinterRespo GetPrinterById(int Id) {
        PrinterRespo array_list = null;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from " + TABLE_PRINTER + " where id =?",
                new String[]{String.valueOf(Id)}, null);
        res.moveToFirst();
        if (res.getCount() > 0) {
            array_list = new PrinterRespo(
                    res.getInt(res.getColumnIndex("id")),
                    res.getString(res.getColumnIndex("name")),
                    res.getInt(res.getColumnIndex("connection")),
                    res.getInt(res.getColumnIndex("print")),
                    res.getString(res.getColumnIndex("ip")),
                    res.getString(res.getColumnIndex("port")),
                    res.getString(res.getColumnIndex("port_add")),
                    res.getString(res.getColumnIndex("indexs"))
            );
        }
        return array_list;
    }

    public ArrayList<GetTables> GetTables(int type) {
        ArrayList<GetTables> array_list = new ArrayList<GetTables>();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = (type == 0) ?
                db.rawQuery("select * from " + TABLE_TABLE_NAME + " ORDER BY tab_type ASC",
                        null) :
                db.rawQuery("select * from " + TABLE_TABLE_NAME +
                                " where tab_type=" + type,
                        null);
        res.moveToFirst();

        while (!res.isAfterLast()) {
            array_list.add(new GetTables(
                            res.getInt(res.getColumnIndex("id")),
                            res.getInt(res.getColumnIndex("status")),
                            res.getString(res.getColumnIndex("tab_label")),
                            res.getInt(res.getColumnIndex("tab_type")))
            );
            res.moveToNext();
        }
        res.close();
        return array_list;
    }
  public GetTables GetTablesById(int id) {
      GetTables array_list = null;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from " + TABLE_TABLE_NAME + " where id=" + id, null);
        res.moveToFirst();

        while (!res.isAfterLast()) {
            array_list=new GetTables(
                            res.getInt(res.getColumnIndex("id")),
                            res.getInt(res.getColumnIndex("status")),
                            res.getString(res.getColumnIndex("tab_label")),
                            res.getInt(res.getColumnIndex("tab_type")));
            res.moveToNext();
        }
        res.close();
        return array_list;
    }

    public void deleteItemOfTable(int tableID) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_ITEM + " WHERE table_id = " + tableID);
        db.close();
    }

    public void deleteItemOfTable(int tableID, int itemId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_ITEM + " WHERE table_id = " + tableID + " AND item_id = " + itemId);
        db.close();
    }

    

    public Integer deleteAllTables() {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_TABLE_NAME, null, null);
    }

    public Integer deleteAllPriceTemplate() {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_PRICE_TEMPLATE, null, null);
    }

    public Integer deleteAllPrinter() {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_PRINTER, null, null);
    }

    public Integer deleteAllMenuSyncCategory() {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(MENU_SYNC_CATEGORY_TABLE_NAME, null, null);
    }

    public Integer deleteAllSyncItems() {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(MENU_SYNC_ITEM_TABLE_NAME, null, null);
    }

    public Integer deleteAllKitCat() {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_KIT_CAT, null, null);
    }

}

