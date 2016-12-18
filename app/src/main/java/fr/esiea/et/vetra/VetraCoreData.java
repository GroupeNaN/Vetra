package fr.esiea.et.vetra;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQuery;
import android.database.sqlite.SQLiteStatement;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/*************************************************************
 **															**
 **			____   ____      __                 			**
 **			\   \ /   /_____/  |_____________   			**
 **			 \   Y   // __ \   __\_  __ \__  \  			**
 **			  \     /\  ___/|  |  |  | \// __ \_			**
 **		 	   \___/  \___  >__|  |__|  (____  /			**
 **			              \/                 \/ 			**
 **															**
 **															**
 **************************************************************/

class VetraCoreData {

    private static final String API_URL = "http://api.themoviedb.org/3/";
    private static final String API_KEY = "api_key=369975af62af308cfd3c6c2079c4075b";
    private static final String DB_FILE = "tmdb.db";


    public static final String INTENT_DL_OVER = "weMadeItToAndromeda";
    public static final String INTENT_DL_DETAILED_OVER = "finallyDone";
    public static final String INTENT_THUMB_AVAILABLE = "inHisHouseAtR'lyehItWaitsDreaming";
    public static Map<Number, String> genres = null;

    private static Context context;

    public VetraCoreData(Context mContext)
    {
        context = mContext;

        try
        {
            String genresEarly = new String(Base64.decode(context.getResources().getString(R.string.genres), Base64.DEFAULT), "UTF-8");
            JSONArray array = new JSONObject(genresEarly).getJSONArray("genres");

            genres = new HashMap<>();

            for(int i = 0; i < array.length(); i++)
            {
                JSONObject object = array.getJSONObject(i);
                genres.put(object.getInt("id"), object.getString("name"));
            }

            SQLiteDatabase database = getDatabase();
            Cursor cursor = database.rawQuery("SELECT " + VetraSqliteNames.MTSCN_ID + " FROM " + VetraSqliteNames.MAIN_TABLE_NAME_SERIES + " LIMIT 1", null);
            if(!cursor.moveToFirst())
                updateData();

            cursor.close();
            database.close();

        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public VetraCoreData()
    {
        if(context == null)
            throw new IllegalArgumentException();
    }

    //High level API
    public void updateData()
    {
        performDownload(API_URL + "discover/tv/?sort_by=popularity.desc&" + context.getString(R.string.url_translation) + "&" + API_KEY);
    }

    private void performDownload(String URL)
    {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && !networkInfo.isConnected())
        {
            Toast.makeText(context, context.getResources().getString(R.string.no_network_error), Toast.LENGTH_SHORT).show();
            return;
        }

        new VetraAsyncDownloader(this).execute(URL);
    }

    static VetraDataStructure getDataWithID(int ID)
    {
        VetraDataStructure output = null;

        SQLiteDatabase database = getDatabase();

        if(database != null)
        {
            String query = "SELECT * FROM " + VetraSqliteNames.MAIN_TABLE_NAME_SERIES +
                    " WHERE " + VetraSqliteNames.MTSCN_ID + " = " + Integer.toString(ID) +
                    " ORDER BY " + VetraSqliteNames.MTSCN_RANK + " LIMIT 1;";

            Cursor cursor = database.rawQuery(query, null);

            if(cursor.getCount() == 1)
            {
                cursor.moveToFirst();
                output = new VetraDataStructure(cursor);
            }

            cursor.close();
            database.close();
        }

        return output;
    }

    public void prepareNewDetailedData(int ID)
    {
        performDownload(API_URL + "tv/" + Integer.toString(ID) + "?" + context.getString(R.string.url_translation) + "&" + API_KEY);
    }

    public static void addToFavs(int ID)
    {
        SQLiteDatabase database = getDatabase();
        if(database != null)
        {
            database.execSQL("INSERT INTO " + VetraSqliteNames.TABLE_FAVORITES +
                    " (" + VetraSqliteNames.FAV_ID + ") values (" + Integer.toString(ID) + ");");
            database.close();

            signalFavUpdate();
        }
    }

    public static void removeFromFavs(int ID)
    {
        SQLiteDatabase database = getDatabase();
        if(database != null)
        {
            database.execSQL("DELETE FROM " + VetraSqliteNames.TABLE_FAVORITES +
                    " WHERE " + VetraSqliteNames.FAV_ID + " = " + Integer.toString(ID));
            database.close();

            signalFavUpdate();
        }
    }

    private static void signalFavUpdate()
    {
        Intent intent = new Intent(INTENT_DL_OVER);
        intent.putExtra("favs", 42);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    protected static void performSearch(String search, VetraSearchActivity callback)
    {
        ArrayList<VetraDataStructure> output = null;
        String URL = null;
        try
        {
            URL = API_URL + "search/tv?query=" + URLEncoder.encode(search, "UTF-8") +
                    "&" + context.getString(R.string.url_translation) + "&" + API_KEY;
        } catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }

        new VetraAsyncSearchDownloader(callback).execute(URL);
    }

    //Internal Parsing
    private ArrayList<VetraDataStructure> parseData(String response)
    {
        if(response == null)
        {
            Toast.makeText(context, context.getResources().getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            return null;
        }

        ArrayList<VetraDataStructure> outputData = null;
        ArrayList<String> postersURL = new ArrayList<>();

        try
        {
            JSONObject json = new JSONObject(response);
            Number page;
            JSONArray results;

            if(json.has("page"))    //discover API call
            {
                page = (Number) json.get("page");
                results = json.getJSONArray("results");
            }
            else
            {
                page = null;
                results = new JSONArray();
                results.put(json);
            }

            outputData = new ArrayList<>();

            for(int count = 0; count < results.length(); count++)
            {
                VetraDataStructure structure = new VetraDataStructure((JSONObject) results.get(count));

                if(structure.initialized)
                {
                    ArrayList<String> fileURL = new ArrayList<>(), size = new ArrayList<>();

                    if(structure.containsFull)
                    {
                        File file = new File(context.getCacheDir(), structure.backdropURL);
                        if(!file.exists())
                        {
                            size.add("/w780");
                            fileURL.add(structure.backdropURL);
                        }

                        //Get the seasons posters
                        for(Map<String, ?> season : structure.seasons)
                        {
                            file = new File(context.getCacheDir(), (String) season.get("posterURL"));
                            if(!file.exists())
                            {
                                size.add("/w92");
                                fileURL.add((String) season.get("posterURL"));
                            }
                        }
                    }
                    else
                    {
                        size.add("/w92");
                        fileURL.add(structure.posterURL);
                    }

                    for(int i = 0; i < fileURL.size(); i++)
                    {
                        File file = new File(context.getCacheDir(), fileURL.get(i));
                        if(!file.exists())
                            postersURL.add("https://image.tmdb.org/t/p" + size.get(i) + fileURL.get(i));
                    }

                    if(page != null)
                        structure.rank = (page.intValue() - 1) * 20 + (count + 1);
                    else
                        structure.rank = -1;

                    outputData.add(structure);
                }
            }

            new VetraAsyncFileDownloader().execute(postersURL);
        }
        catch (Exception e)
        {
            Toast.makeText(context, context.getResources().getString(R.string.parse_error), Toast.LENGTH_SHORT).show();
        }

        return outputData;
    }

    private void writeDataToDatabase(ArrayList<VetraDataStructure> data)
    {
        boolean specificEntry = data.size() == 1 && data.get(0).containsFull;

        SQLiteDatabase database = getDatabase();

        if(database != null)
        {
            SQLiteStatement statement;

            if(specificEntry)
                statement = getFullInsertionStatement(database);
            else
                statement = getPartialInsertionStatement(database);

            if(statement != null)
            {
                for(VetraDataStructure item : data)
                {
                    if(!specificEntry)
                    {
                        database.execSQL("DELETE FROM " + VetraSqliteNames.MAIN_TABLE_NAME_SERIES + " WHERE (" +
                                VetraSqliteNames.MTSCN_ID + " = " + item.ID + " AND " + VetraSqliteNames.MTSCN_RANK + " != -1) OR " +
                                VetraSqliteNames.MTSCN_RANK + " = " + item.rank + ";");
                    }

                    if(item.serializeToDB(statement, specificEntry))
                    {
                        statement.execute();
                        statement.clearBindings();
                    }
                }
                statement.close();
            }
            database.close();
        }
    }

    //Downloader
    protected static class VetraAsyncDownloader extends AsyncTask<String, Void, String>
    {
        protected final VetraCoreData callback;

        public VetraAsyncDownloader()
        {
            callback = null;
        }

        public VetraAsyncDownloader(VetraCoreData callback)
        {
            this.callback = callback;
        }

        @Override
        protected String doInBackground(String... urls) {

            // params comes from the execute() call: params[0] is the url.
            return downloadUrl(urls[0]);
        }

        private String downloadUrl(String URL)
        {
            try
            {
                HttpURLConnection conn;

                //Somewhat deal with redirections...
                do
                {
                    URL url = new URL(URL);

                    conn = (HttpURLConnection) url.openConnection();

                    conn.setRequestMethod("GET");
                    conn.setDoInput(true);

                    conn.connect();

                    if(conn.getResponseCode() == 301 || conn.getResponseCode() == 302)
                    {
                        URL = conn.getHeaderField("Location");
                    }
                    else
                    {
                        break;
                    }

                } while(true);

                BufferedReader streamReader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder responseStrBuilder = new StringBuilder();

                String inputStr;
                while ((inputStr = streamReader.readLine()) != null)
                    responseStrBuilder.append(inputStr);

                conn.disconnect();

                return responseStrBuilder.toString();
            }
            catch (IOException e)
            {
                return null;
            }
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String  result)
        {
            ArrayList<VetraDataStructure> output = this.callback.parseData(result);

            if(output != null)
            {
                this.callback.writeDataToDatabase(output);

                if(!output.get(0).containsFull)
                    LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(INTENT_DL_OVER));
                else
                    LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(INTENT_DL_DETAILED_OVER));
            }
        }
    }

    //Downloader
    private static class VetraAsyncSearchDownloader extends VetraAsyncDownloader
    {
        VetraSearchActivity searchActivity;

        public VetraAsyncSearchDownloader(VetraSearchActivity callback)
        {
            super();
            searchActivity = callback;
        }

        @Override
        protected void onPostExecute(String result)
        {
            if(result == null)
                return;

            ArrayList<VetraDataStructure> output = new ArrayList<>();

            try
            {
                JSONObject jsonObject = new JSONObject(result);
                JSONArray jsonArray = jsonObject.getJSONArray("results");

                for(int i = 0; i < jsonArray.length(); i++)
                {
                    jsonObject = jsonArray.getJSONObject(i);

                    VetraDataStructure data = new VetraDataStructure();

                    data.ID = jsonObject.getInt("id");
                    data.name = jsonObject.getString("name");
                    data.initialAir = jsonObject.getString("first_air_date");

                    output.add(data);
                }

                searchActivity.getNewData(output);
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
        }
    }

    //Downloader
    private class VetraAsyncFileDownloader extends AsyncTask<ArrayList<String>, Void, Void>
    {
        @Override
        protected Void doInBackground(ArrayList<String>... urls) {

            // params comes from the execute() call: params[0] is the url.
            for(String URL : urls[0])
            {
                downloadUrl(URL);
            }

            return null;
        }

        private void downloadUrl(String URL)
        {

            try
            {
                HttpURLConnection conn;

                //Somewhat deal with redirections...
                do
                {
                    URL url = new URL(URL);

                    conn = (HttpURLConnection) url.openConnection();

                    conn.setRequestMethod("GET");
                    conn.setDoInput(true);

                    conn.connect();

                    if(conn.getResponseCode() == 301 || conn.getResponseCode() == 302)
                    {
                        URL = conn.getHeaderField("Location");
                    }
                    else
                    {
                        break;
                    }

                } while(true);

                BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                //We create an array of bytes
                byte[] data = new byte[256];
                int current;

                while((current = bis.read(data,0,data.length)) != -1)
                {
                    buffer.write(data,0,current);
                }

                FileOutputStream fos = new FileOutputStream(new File(context.getCacheDir(), URL.substring(URL.lastIndexOf('/') + 1)));
                fos.write(buffer.toByteArray());
                fos.close();

                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(INTENT_THUMB_AVAILABLE));

                conn.disconnect();
            }
            catch (IOException ignored)
            {}
        }
    }

    //Database proxy
    public static SQLiteDatabase getDatabase()
    {
        SQLiteDatabase.CursorFactory cursorFactory = new SQLiteDatabase.CursorFactory()
        {
            @Override
            public Cursor newCursor(SQLiteDatabase db, SQLiteCursorDriver masterQuery, String editTable, SQLiteQuery query)
            {
                return new SQLiteCursor(masterQuery, editTable, query);
            }
        };

        SQLiteDatabase output = context.openOrCreateDatabase(DB_FILE, Context.MODE_PRIVATE, cursorFactory);

        //If database is corrupted, we crash it
        if(output != null && !output.isDatabaseIntegrityOk())
        {
            output.close();
            context.getDatabasePath(VetraCoreData.DB_FILE).delete();
            output = context.openOrCreateDatabase(DB_FILE, Context.MODE_PRIVATE, cursorFactory);
        }

        //Create schema if necessary
        output.execSQL("CREATE TABLE IF NOT EXISTS " + VetraSqliteNames.MAIN_TABLE_NAME_SERIES + "(" +
                VetraSqliteNames.MTSCN_ID + " INTEGER NOT NULL, " +
                VetraSqliteNames.MTSCN_HAS_FULL + " INTEGER NOT NULL, " +
                VetraSqliteNames.MTSCN_RANK + " INTEGER NOT NULL, " +
                VetraSqliteNames.MTSCN_NAME + " TEXT NOT NULL, " +
                VetraSqliteNames.MTSCN_BASE_NAME + " TEXT NOT NULL, " +
                VetraSqliteNames.MTSCN_SYNOPSIS + " TEXT NOT NULL, " +
                VetraSqliteNames.MTSCN_INITIAL_AIR + " TEXT NOT NULL, " +
                VetraSqliteNames.MTSCN_ORIGIN_COUNTRY + " TEXT NOT NULL, " +
                VetraSqliteNames.MTSCN_GENRES + " TEXT NOT NULL, " +
                VetraSqliteNames.MTSCN_LANGUAGE + " TEXT NOT NULL, " +
                VetraSqliteNames.MTSCN_POSTER_URL + " TEXT NOT NULL, " +
                VetraSqliteNames.MTSCN_BACKDROP_URL + " TEXT NOT NULL, " +
                VetraSqliteNames.MTSCN_POPULARITY + " TEXT NOT NULL, " +
                VetraSqliteNames.MTSCN_VOTE_AVERAGE + " FLOAT NOT NULL, " +
                VetraSqliteNames.MTSCN_VOTE_COUNT + " INTEGER NOT NULL, " +
                VetraSqliteNames.MTSCN_CREATOR_ID + " INTEGER, " +
                VetraSqliteNames.MTSCN_CREATOR_NAME + " TEXT, " +
                VetraSqliteNames.MTSCN_CREATOR_IMAGE + " TEXT, " +
                VetraSqliteNames.MTSCN_EPISODE_DURATION + " TEXT, " +
                VetraSqliteNames.MTSCN_WEBSITE + " TEXT, " +
                VetraSqliteNames.MTSCN_CHANNEL + " TEXT, " +
                VetraSqliteNames.MTSCN_STATUS + " TEXT, " +
                VetraSqliteNames.MTSCN_TYPE + " TEXT, " +
                VetraSqliteNames.MTSCN_NUMBER_SEASONS + " INTEGER, " +
                VetraSqliteNames.MTSCN_NUMBER_EPISODES + " INTEGER, " +
                VetraSqliteNames.MTSCN_SEASONS + " TEXT, " +
                VetraSqliteNames.MTSCN_PRODUCTION + " TEXT, " +
                VetraSqliteNames.MTSCN_STILL_RUNNING + " INTEGER);");

        output.execSQL("CREATE TABLE IF NOT EXISTS " + VetraSqliteNames.TABLE_FAVORITES +
                "(" + VetraSqliteNames.FAV_ID + " INTEGER NOT NULL);");


        return output;
    }

    private SQLiteStatement getFullInsertionStatement(SQLiteDatabase database)
    {
        return database.compileStatement("INSERT INTO " + VetraSqliteNames.MAIN_TABLE_NAME_SERIES + "(" +
                VetraSqliteNames.MTSCN_ID + "," +
                VetraSqliteNames.MTSCN_HAS_FULL + "," +
                VetraSqliteNames.MTSCN_RANK + "," +
                VetraSqliteNames.MTSCN_NAME + "," +
                VetraSqliteNames.MTSCN_BASE_NAME + "," +
                VetraSqliteNames.MTSCN_SYNOPSIS + "," +
                VetraSqliteNames.MTSCN_INITIAL_AIR + "," +
                VetraSqliteNames.MTSCN_ORIGIN_COUNTRY + "," +
                VetraSqliteNames.MTSCN_GENRES + "," +
                VetraSqliteNames.MTSCN_LANGUAGE + "," +
                VetraSqliteNames.MTSCN_POSTER_URL + "," +
                VetraSqliteNames.MTSCN_BACKDROP_URL + "," +
                VetraSqliteNames.MTSCN_POPULARITY + "," +
                VetraSqliteNames.MTSCN_VOTE_AVERAGE + "," +
                VetraSqliteNames.MTSCN_VOTE_COUNT + "," +
                VetraSqliteNames.MTSCN_CREATOR_ID + "," +
                VetraSqliteNames.MTSCN_CREATOR_NAME + "," +
                VetraSqliteNames.MTSCN_CREATOR_IMAGE + "," +
                VetraSqliteNames.MTSCN_EPISODE_DURATION + "," +
                VetraSqliteNames.MTSCN_WEBSITE + "," +
                VetraSqliteNames.MTSCN_CHANNEL + "," +
                VetraSqliteNames.MTSCN_STATUS + ", " +
                VetraSqliteNames.MTSCN_TYPE + ", " +
                VetraSqliteNames.MTSCN_NUMBER_SEASONS + "," +
                VetraSqliteNames.MTSCN_NUMBER_EPISODES + "," +
                VetraSqliteNames.MTSCN_SEASONS + "," +
                VetraSqliteNames.MTSCN_STILL_RUNNING + "," +
                VetraSqliteNames.MTSCN_PRODUCTION + ") " +
                "values (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, " +
                "?15, ?16, ?17, ?18, ?19, ?20, ?21, ?22, ?23, ?24, ?25, ?26, ?27, ?28);");
    }

    private SQLiteStatement getPartialInsertionStatement(SQLiteDatabase database)
    {
        return database.compileStatement("INSERT INTO " + VetraSqliteNames.MAIN_TABLE_NAME_SERIES + "(" +
                VetraSqliteNames.MTSCN_ID + "," +
                VetraSqliteNames.MTSCN_HAS_FULL + "," +
                VetraSqliteNames.MTSCN_RANK + "," +
                VetraSqliteNames.MTSCN_NAME + "," +
                VetraSqliteNames.MTSCN_BASE_NAME + "," +
                VetraSqliteNames.MTSCN_SYNOPSIS + "," +
                VetraSqliteNames.MTSCN_INITIAL_AIR + "," +
                VetraSqliteNames.MTSCN_ORIGIN_COUNTRY + "," +
                VetraSqliteNames.MTSCN_GENRES + "," +
                VetraSqliteNames.MTSCN_LANGUAGE + "," +
                VetraSqliteNames.MTSCN_POSTER_URL + "," +
                VetraSqliteNames.MTSCN_BACKDROP_URL + "," +
                VetraSqliteNames.MTSCN_POPULARITY + "," +
                VetraSqliteNames.MTSCN_VOTE_AVERAGE + "," +
                VetraSqliteNames.MTSCN_VOTE_COUNT + ") " +
                "values (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15);");
    }
}
