package com.goodow.drive.android.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * 数据库以及表创建和更新
 * 
 * @author dpw
 * 
 */

public class DBHelper extends SQLiteOpenHelper {

  public static final String DBNAME = "keruixing";

  public static DBHelper getInstance(Context context) {
    return new DBHelper(context);
  }

  private DBHelper(Context context) {
    super(context, DBNAME, null, 1);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {

    /*
     * 创建文件表
     */
    db.execSQL("CREATE TABLE T_FILE(UUID varchar(80) primary key NOT NULL ,"
        + "NAME varchar(200) NOT NULL ,"
        + "CONTENTTYPE varchar(300) DEFAULT NULL ,SIZE int(11) NOT NULL ,"
        + "FILEPATH varchar(500) NOT NULL ,THUMBNAILS varchar(500) DEFAULT NULL ,"
        + "CREATETIME varchar(45) DEFAULT NULL ,UPDATETIME varchar(45) DEFAULT NULL )");

    /*
     * 创建标签映射表
     */
    db.execSQL("CREATE TABLE T_RELATION(TYPE varchar(80) NOT NULL ,"
        + "KEY varchar(80) NOT NULL ,TAG varchar(80) NOT NULL ,"
        + "CREATETIME varchar(45) DEFAULT NULL ,UPDATETIME varchar(45) DEFAULT NULL, PRIMARY KEY (TYPE,KEY,TAG))");

    /*
     * 创建收藏映射表
     */
    db.execSQL("CREATE TABLE T_STAR(TYPE varchar(80) NOT NULL, "
        + "TAG varchar(500) NOT NULL ,USER_ID varchar(80) DEFAULT NULL ,"
        + "CREATETIME varchar(45) DEFAULT NULL ,UPDATETIME varchar(45) DEFAULT NULL, PRIMARY KEY (TYPE,TAG))");

    /**
     * 创建开机数据统计
     */
    db.execSQL("CREATE TABLE T_BOOT(" + "ID INTEGER PRIMARY KEY AUTOINCREMENT,"
        + "OPEN_TIME long NOT NULL," + "LAST_TIME long NOT NULL," + "CLOSE_TIME long NOT NULL,"
        + "LATITUDE  long, " + "LONGITUDE long," + "RADIUS long, " + "ADDRESS vachar(45))");

    /*
     * 创建播放数据统计
     */
    db.execSQL("CREATE TABLE T_PLAYER(" + "ID INTEGER PRIMARY KEY AUTOINCREMENT,"
        + "FILE_NAME varchar(80) NOT NULL, " + "OPEN_TIME long NOT NULL, "
        + "LAST_TIME long NOT NULL )");

  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

  }

}
