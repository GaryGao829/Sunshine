package com.example.yugao.sunshine.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;




/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {
    private ArrayAdapter<String> mForecastAdapter;


    public ForecastFragment() {
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    public void onCreateOptionsMenu(Menu menu,MenuInflater inflater){
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    /**
     * 处理菜单中选项被点击时触发的动作
     * @param item
     * @return
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        /**
         * 当Refresh被点击时候触发的内容
         */
        if (id == R.id.action_refresh) {
            FetchWeatherTask weatherTask = new FetchWeatherTask();
            weatherTask.execute("94043");
            //weatherTask.execute();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        //先用假数据进行填充
        String [] forecastArray = {
                "Today - Sunny - 88/63",
                "Tomorrow - Foggy - 70/46",
                "Weds - Cloudy - 72/63",
                "Thurs - Rainy - 64/51",
                "Fri - Foggy - 70/46",
                "Sat - Sunny - 75/68"
        };
        //把天气数组转化成List
        List<String> weekForecast = new ArrayList<String>(Arrays.asList(forecastArray));

        //构造所需要的适配器
        mForecastAdapter = new ArrayAdapter<String>(
                getActivity(),  //当前的Context
                R.layout.list_item_forecast,      //Layout 文件的Resource ID
                R.id.list_item_forecast_textview, //待被填充的 Layout中的 TextView的ID
                weekForecast                        //要在ListView中显示的内容
        );

        //在制定ListView中 设置这个适配器
        ListView listView = (ListView) rootView.findViewById(
                R.id.listview_forecast);
        listView.setAdapter(mForecastAdapter);

        //给ListView设置触发器
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            //在ListView 中的Item被点击后进行Activity跳转
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Context context = mForecastAdapter.getContext();
                String weatherinfo = mForecastAdapter.getItem(position).toString();
                //Toast toast = Toast.makeText(context, weatherinfo, Toast.LENGTH_SHORT);
                //toast.show();
                Intent sendIntent = new Intent(getActivity(), DetailActivity.class);
                sendIntent.putExtra(Intent.EXTRA_TEXT, weatherinfo);
                startActivity(sendIntent);
            }
        });

        return rootView;


    }


    /**
     * 构建这个类用于异步获取天气信息
     * 如果放在同一个线程中执行的话会导致UI卡顿 影响用户体验
     *
     *     AsyncTask是一个封装好的多线程类 用于在后台执行 简单的,耗时少的 操作然后对UI进行更新 不用手动操作多线程以及handlers
     *     如果要长时间保持一个进程 或者要执行一个耗时较长的操作 那么Google的建议是结合 Executor, ThreadPoolExecutor and FutureTask 这三个类的API
     *
     */
class FetchWeatherTask extends AsyncTask<String, Integer, String[]> {

    final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
    String[] WeatherList;

    @Override
    /**
     * Override这个方法，实现
     * 调用API获取天气信息
     */
        public String[] doInBackground(String... params){
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            String format = "json";
            String units = "metric";
            int numDays = 7;
            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are available at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                final String FORECAST_BASE_URL ="http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String QUERY_PARAM = "q";
                final String FORMAT_PARAM = "mode";
                final String UNITS_PARAM = "units";
                final String DAYS_PARAM = "cnt";
                //构造Http请求
                Uri buildUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM,params[0])
                        .appendQueryParameter(FORMAT_PARAM,format)
                        .appendQueryParameter(UNITS_PARAM,units)
                        .appendQueryParameter(DAYS_PARAM,Integer.toString(numDays))
                        .build();
                //得到构造好的URL
                URL url = new URL(buildUri.toString());

                Log.v(LOG_TAG,"Built URI"+buildUri.toString());

                //创建对OpenWeatherAPI的Http请求
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                Log.v(LOG_TAG, "before connect");
                urlConnection.connect();
                Log.v(LOG_TAG, "connect succeed");

                //得到返回的输入流
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    forecastJsonStr = null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    //因为是JSON 所以添加换行并不会对内容读取造成问题
                    //但是这样会让debug的时候看起来很方便
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    forecastJsonStr = null;
                }
                forecastJsonStr = buffer.toString();
                Log.v(LOG_TAG,"Forecast JSON String: "+ forecastJsonStr);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attempting
                // to parse it.
                forecastJsonStr = null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                    Log.v(LOG_TAG,"urlConnection disconnect");
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e("PlaceholderFragment", "Error closing stream", e);
                    }
                }
            }
        try {
           //forecastJsonStr 是Http请求返回得到的流（读取时候加入了\n） 这是一个JSON对象
            WeatherList = getWeatherDataFromJson(forecastJsonStr,numDays);
        }catch (JSONException e){
            Log.e(LOG_TAG,"Error",e);
            e.printStackTrace();
        }
        return WeatherList;
        //return null;
    }

    private String getReadableDateString(long time){
        //translate time from Unix time stamp to readable format "EEE MMM dd"
        SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
        return shortenedDateFormat.format(time);
    }

    private String formatHighLows(double high,double low){
        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);

        String highLowStr = roundedHigh + "/" + roundedLow;
        return highLowStr;
        }


    /**
     * 把一个JSON对象解析的过程
     * @param forecastJsonStr
     * @param numDays
     * @return String[]
     * @throws JSONException
     */
    private String[] getWeatherDataFromJson(String forecastJsonStr,int numDays) throws JSONException{
        final String OWM_LIST = "list";
        final String OWM_WEATHER = "weather";
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";
        final String OWM_DESCRIPTION = "main";
        JSONObject forecastJson = new JSONObject(forecastJsonStr);
        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

        Time dayTime = new Time();
        dayTime.setToNow();
        int julianStartDay = Time.getJulianDay(System.currentTimeMillis(),dayTime.gmtoff);
        dayTime = new Time();

        String[] resultStrs = new String[numDays];
        //遍历一个JsonArray，解析每一项JSONObject 赋值给一个String[]
        for(int i = 0;i<weatherArray.length();i++){
            String day;
            String description;
            String highAndLow;

            JSONObject dayForecast = weatherArray.getJSONObject(i);

            long dateTime;
            dateTime = dayTime.setJulianDay(julianStartDay + i);
            day = getReadableDateString(dateTime);

            JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            description = weatherObject.getString(OWM_DESCRIPTION);

            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
            double high = temperatureObject.getDouble(OWM_MAX);
            double low = temperatureObject.getDouble(OWM_MIN);

            highAndLow = formatHighLows(high,low);
            resultStrs[i] = day + " - " + description + " - " + highAndLow;
        }
        for(String s:resultStrs){
            Log.v(LOG_TAG,"Forecast entry " + s);
        }
        return resultStrs;
    }

    @Override
    //用于反馈后台操作的状态更新 比如下载时候显示进度条
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
    }

    @Override
    //把后台操作完成以后的结果 更新到适配器中
    protected void onPostExecute(String[] strings) {
        mForecastAdapter.clear();
        //mForecastAdapter.addAll(strings); this is not available in low version android(Android 3.0)
        if(!strings.equals(null)) {
            for (String weather : strings) {
                mForecastAdapter.add(weather);
                /**
                 * 这里有个细节：
                 *     Q:为什么在修改了适配器里面要填充的数据 UI所显示的会自动更改？
                 *
                 *     A:ArrayAdapter类中有一个变量boolean是 notifyOnChange 默认为true 此时如果这个
                 *       ArrayAdapter对象执行了add(T), insert(T, int), remove(T), clear() 这些操作的时候会调用 notifyDataSetChanged() 用来通知UI
                 *       要刷新内容。
                 *       这个自动化的过程也可以通过设置 notifyOnChange 这个变量为false来关闭 这是就要手动调用notifyDataSetChanged()来更新UI
                 *       所以这里我们只要用了add()就会自动更新UI数据。
                 */
            }
        }
    }
}
}