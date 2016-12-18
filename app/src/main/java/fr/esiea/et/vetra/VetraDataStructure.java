package fr.esiea.et.vetra;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

class VetraDataStructure
{
    public boolean initialized = false;
    public boolean containsFull = false;
    public boolean minimal = false;
    public Number ID;

    public Number rank;

    public String name;
    private String originalName;
    public String synopsis;
    public String initialAir;

    private ArrayList<String> origin;
    public ArrayList genres;

    public String language;

    public String posterURL, backdropURL;
    private Number popularity;
    public Number vote;
    private Number voteCount;

    //Creator
    private Number creatorID;
    public String creatorName;
    private String creatorProfileURL;

    public String productorName;

    private ArrayList<Number> episodeDuration;
    public String homepageURL;
    public String channel;
    private String status;
    private String type;

    public Number numberSeason, numberEpisodes;
    public ArrayList<Map<String, ?>> seasons;

    private boolean stillRunning;

    private static final String DB_JSON_CONTAINER = "data";

    public VetraDataStructure()
    {
        minimal = true;
    }

    public VetraDataStructure(JSONObject jsonData) throws JSONException
    {
        ID = (Number) jsonData.get("id");

        name = (String) jsonData.get("name");
        originalName = (String) jsonData.get("original_name");
        synopsis = (String) jsonData.get("overview");
        initialAir = (String) jsonData.get("first_air_date");

        origin = jsonToArray(jsonData.getJSONArray("origin_country"));

        language = (String) jsonData.get("original_language");

        posterURL = (String) jsonData.get("poster_path");

        if(!jsonData.get("backdrop_path").toString().equals("null"))
            backdropURL = (String) jsonData.get("backdrop_path");
        else
            backdropURL = "/r8qkc5No5PC75x88PJ5vEdwwQpX.jpg";   //better something than nothing

        vote = (Number) jsonData.get("vote_average");
        voteCount = (Number) jsonData.get("vote_count");
        popularity = (Number) jsonData.get("popularity");


        //Data past this point are only available from the detailled API access point
        if(jsonData.has("created_by"))
        {
            containsFull = true;

            //Creator
            JSONObject creator = (JSONObject) jsonData.getJSONArray("created_by").get(0);
            creatorID = (Number) creator.get("id");
            creatorName = creator.getString("name");

            if(!creator.get("profile_path").toString().equals("null"))
                creatorProfileURL = creator.getString("profile_path");
            else
                creatorProfileURL = "/r8qkc5No5PC75x88PJ5vEdwwQpX.jpg";   //better something than nothing

            productorName = ((JSONObject) jsonData.getJSONArray("production_companies").get(0)).getString("name");

            episodeDuration = jsonToArray(jsonData.getJSONArray("episode_run_time"));

            genres = new ArrayList<>();
            JSONArray array = jsonData.getJSONArray("genres");
            for(int i = 0; i < array.length(); i++)
                genres.add(((JSONObject) array.get(i)).get("id"));

            homepageURL = (String) jsonData.get("homepage");
            channel = (String) ((JSONObject) jsonData.getJSONArray("networks").get(0)).get("name");

            stillRunning = (Boolean) jsonData.get("in_production");

            numberSeason = (Number) jsonData.get("number_of_seasons");
            numberEpisodes = (Number) jsonData.get("number_of_episodes");

            seasons = new ArrayList<>();
            array = jsonData.getJSONArray("seasons");
            for(int i = 0; i < array.length(); i++)
            {
                JSONObject seasonNode = array.getJSONObject(i);
                HashMap<String, Object> season = new HashMap<>();

                season.put("seasonNumber", seasonNode.getLong("season_number"));
                season.put("id", seasonNode.getLong("id"));
                season.put("posterURL", seasonNode.getString("poster_path"));
                season.put("episodeCount", seasonNode.getLong("episode_count"));
                season.put("initialAir", seasonNode.getString("air_date"));

                seasons.add(season);
            }

            type = (String) jsonData.get("type");
            status = (String) jsonData.get("status");
        }
        else
        {
            genres = jsonToArray(jsonData.getJSONArray("genre_ids"));
        }

        initialized = true;
    }

    public VetraDataStructure(Cursor cursor)
    {
        try
        {
            ID = cursor.getLong(cursor.getColumnIndex(VetraSqliteNames.MTSCN_ID));

            containsFull = cursor.getLong(cursor.getColumnIndex(VetraSqliteNames.MTSCN_HAS_FULL)) != 0;
            rank = cursor.getLong(cursor.getColumnIndex(VetraSqliteNames.MTSCN_RANK));

            name = cursor.getString(cursor.getColumnIndex(VetraSqliteNames.MTSCN_BASE_NAME));
            originalName = cursor.getString(cursor.getColumnIndex(VetraSqliteNames.MTSCN_BASE_NAME));
            synopsis = cursor.getString(cursor.getColumnIndex(VetraSqliteNames.MTSCN_SYNOPSIS));

            initialAir = cursor.getString(cursor.getColumnIndex(VetraSqliteNames.MTSCN_INITIAL_AIR));

            origin = jsonToArray(new JSONObject(cursor.getString(cursor.getColumnIndex(VetraSqliteNames.MTSCN_ORIGIN_COUNTRY))).getString(DB_JSON_CONTAINER));
            genres = jsonToArray(new JSONObject(cursor.getString(cursor.getColumnIndex(VetraSqliteNames.MTSCN_GENRES))).getString(DB_JSON_CONTAINER));

            language = cursor.getString(cursor.getColumnIndex(VetraSqliteNames.MTSCN_LANGUAGE));
            posterURL = cursor.getString(cursor.getColumnIndex(VetraSqliteNames.MTSCN_POSTER_URL));
            backdropURL = cursor.getString(cursor.getColumnIndex(VetraSqliteNames.MTSCN_BACKDROP_URL));
            popularity = cursor.getDouble(cursor.getColumnIndex(VetraSqliteNames.MTSCN_POPULARITY));
            vote = cursor.getDouble(cursor.getColumnIndex(VetraSqliteNames.MTSCN_VOTE_AVERAGE));
            voteCount = cursor.getLong(cursor.getColumnIndex(VetraSqliteNames.MTSCN_VOTE_COUNT));

            if(containsFull)
            {
                backdropURL = cursor.getString(cursor.getColumnIndex(VetraSqliteNames.MTSCN_BACKDROP_URL));
                productorName = cursor.getString(cursor.getColumnIndex(VetraSqliteNames.MTSCN_PRODUCTION));

                creatorID = cursor.getLong(cursor.getColumnIndex(VetraSqliteNames.MTSCN_CREATOR_ID));
                creatorName = cursor.getString(cursor.getColumnIndex(VetraSqliteNames.MTSCN_CREATOR_NAME));
                creatorProfileURL = cursor.getString(cursor.getColumnIndex(VetraSqliteNames.MTSCN_CREATOR_IMAGE));

                episodeDuration = jsonToArray(new JSONObject(cursor.getString(cursor.getColumnIndex(VetraSqliteNames.MTSCN_EPISODE_DURATION))).getString(DB_JSON_CONTAINER));

                homepageURL = cursor.getString(cursor.getColumnIndex(VetraSqliteNames.MTSCN_WEBSITE));
                channel = cursor.getString(cursor.getColumnIndex(VetraSqliteNames.MTSCN_CHANNEL));
                status = cursor.getString(cursor.getColumnIndex(VetraSqliteNames.MTSCN_STATUS));
                type = cursor.getString(cursor.getColumnIndex(VetraSqliteNames.MTSCN_TYPE));
                numberSeason = cursor.getLong(cursor.getColumnIndex(VetraSqliteNames.MTSCN_NUMBER_SEASONS));
                numberEpisodes = cursor.getLong(cursor.getColumnIndex(VetraSqliteNames.MTSCN_NUMBER_EPISODES));

                seasons = new ArrayList<>();
                JSONArray array = new JSONArray(cursor.getString(cursor.getColumnIndex(VetraSqliteNames.MTSCN_SEASONS)));
                for(int i = 0; i < array.length(); i++)
                {
                    JSONObject seasonNode = array.getJSONObject(i);
                    HashMap<String, Object> season = new HashMap<>();

                    season.put("seasonNumber", seasonNode.get("seasonNumber"));
                    season.put("id", seasonNode.get("id"));
                    season.put("posterURL", seasonNode.get("posterURL"));
                    season.put("episodeCount", seasonNode.get("episodeCount"));
                    season.put("initialAir", seasonNode.get("initialAir"));

                    seasons.add(season);
                }

                stillRunning = cursor.getLong(cursor.getColumnIndex(VetraSqliteNames.MTSCN_STILL_RUNNING)) != 0;
            }

            initialized = true;
        }
        catch (Exception e)
        {
            Log.d("Everything is broken", "FML");
        }
    }

    public boolean serializeToDB(SQLiteStatement statement, boolean expectFull)
    {
        JSONObject json = new JSONObject();

        try
        {
            statement.bindLong(1, ID.longValue());
            statement.bindLong(2, containsFull ? 1 : 0);
            statement.bindLong(3, rank.longValue());
            statement.bindString(4, name);
            statement.bindString(5, originalName);
            statement.bindString(6, synopsis);
            statement.bindString(7, initialAir);

            //Arrays and stuffs can't simply be inserted into the DB
            //We don't intend creating a sustainable database so data we won't query in the short term can be serialized in a handy format
            json.put(DB_JSON_CONTAINER, origin);
            statement.bindString(8, json.toString());
            json.remove(DB_JSON_CONTAINER);

            json.put(DB_JSON_CONTAINER, genres);
            statement.bindString(9, json.toString());
            json.remove(DB_JSON_CONTAINER);

            statement.bindString(10, language);
            statement.bindString(11, posterURL);
            statement.bindString(12, backdropURL);
            statement.bindDouble(13, popularity.doubleValue());
            statement.bindDouble(14, vote.doubleValue());
            statement.bindLong(15, voteCount.longValue());

            if(expectFull)
            {
                if(containsFull)
                {
                    statement.bindLong(16, creatorID.longValue());
                    statement.bindString(17, creatorName);
                    statement.bindString(18, creatorProfileURL);

                    json.put(DB_JSON_CONTAINER, episodeDuration);
                    statement.bindString(19, json.toString());
                    json.remove(DB_JSON_CONTAINER);

                    statement.bindString(20, homepageURL);
                    statement.bindString(21, channel);
                    statement.bindString(22, status);
                    statement.bindString(23, type);
                    statement.bindLong(24, numberSeason.longValue());
                    statement.bindLong(25, numberEpisodes.longValue());

                    boolean first = true;
                    String manuallyCraftedJSON = "[";
                    for(Map<String, ?> season : seasons)
                    {
                        if(!first)
                            manuallyCraftedJSON += ",";

                        manuallyCraftedJSON += "{\"seasonNumber\":" + ((Number) season.get("seasonNumber")).intValue() + ",";
                        manuallyCraftedJSON += "\"id\":" + ((Number) season.get("id")).intValue() + ",";
                        manuallyCraftedJSON += "\"posterURL\":\"" + season.get("posterURL") + "\",";
                        manuallyCraftedJSON += "\"episodeCount\":" + ((Number) season.get("episodeCount")).intValue() + ",";
                        manuallyCraftedJSON += "\"initialAir\":\"" + season.get("initialAir") + "\"}";

                        if(first)
                            first = false;
                    }
                    statement.bindString(26, manuallyCraftedJSON + "]");

                    statement.bindLong(27, stillRunning ? 1 : 0);
                    statement.bindString(28, productorName);
                }
                else
                {
                    for(int i = 16; i < 27; i++)
                        statement.bindNull(i);
                }
            }
        }
        catch (Exception e)
        {
            statement.clearBindings();
            return false;
        }

        return true;
    }

    private ArrayList jsonToArray(String json) throws JSONException
    {
        return jsonToArray(new JSONArray(json));
    }

    private ArrayList jsonToArray(JSONArray json) throws JSONException
    {
        ArrayList output = new ArrayList();

        for(int i = 0; i < json.length(); i++)
            output.add(json.get(i));

        return output;
    }
}
