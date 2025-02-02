// Copyright (c) 2014 ReelSonar. All rights reserved.

package com.reelsonar.ibobber.triplog;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.reelsonar.ibobber.BaseActivity;
import com.reelsonar.ibobber.R;
import com.reelsonar.ibobber.TaskListener;
import com.reelsonar.ibobber.db.DBLoader;
import com.reelsonar.ibobber.model.UserAuth.UserAuth;
import com.reelsonar.ibobber.model.triplog.CatchTripListMain;
import com.reelsonar.ibobber.model.triplog.TripLog;
import com.reelsonar.ibobber.util.ApiLoader;
import com.reelsonar.ibobber.util.AppUtils;
import com.reelsonar.ibobber.util.CallBack;
import com.reelsonar.ibobber.util.RestConstants;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import de.greenrobot.event.EventBus;
import retrofit2.Call;
import retrofit2.Response;

import static com.reelsonar.ibobber.util.Actions.NETFISH_ADS_ACTION;
import static com.reelsonar.ibobber.util.Actions.NETFISH_SPLASH_ACTION;
import static com.reelsonar.ibobber.util.RestConstants.CATCH_ID;
import static com.reelsonar.ibobber.util.RestConstants.IBOBBER_APP;
import static com.reelsonar.ibobber.util.RestConstants.ISIBOBBER;
import static com.reelsonar.ibobber.util.RestConstants.NETFISH_CATCH_LOG_ACTIVITY;
import static com.reelsonar.ibobber.util.RestConstants.NETFISH_PACKAGE;
import static com.reelsonar.ibobber.util.RestConstants.OTHER_USERID;
import static com.reelsonar.ibobber.util.RestConstants.USERID;

public class TripLogListActivity extends BaseActivity implements AdapterView.OnItemClickListener {


    ListView lstTrip;
    ImageView imgTripLog, imgGPS;
    TripLogAdapter adapter;
    Date dateToHighlight;
    private CatchTripListMain listMain = null;
    private UserAuth userAuth;
    private ProgressBar progressBar;
    private List<TripLog> tripLogList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_triplog_list);

        progressBar = (ProgressBar) findViewById(R.id.loadingSpinner);
        lstTrip = (ListView) findViewById(R.id.lvTrip);
        lstTrip.setOnItemClickListener(this);
        registerForContextMenu(lstTrip);

        imgTripLog = (ImageView) findViewById(R.id.imgTripLog);
        imgGPS = (ImageView) findViewById(R.id.imgGPS);

        imgGPS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /**
                 * NETFISH_ADS_FLAG = 0 at initial , First time it redirect to netfish play store using appsflyer link
                 * and then next time redirect to NetFishAdsActivity.
                 */
                int netFishCount = AppUtils.showNetFishAds(getApplicationContext());
                if (netFishCount == 0) {
                    Intent in = new Intent(NETFISH_SPLASH_ACTION);
                    startActivity(in);
                } else if (netFishCount > 0 && netFishCount < 3) {
                    Intent in = new Intent(NETFISH_ADS_ACTION);
                    startActivity(in);
                } else {
                    TripLog newTripLog = TripLogService.getInstance(getApplicationContext()).saveTripLogAtCurrentLocation();
                    TripLog addedTripLog = TripLogService.getInstance(getApplicationContext()).saveTripLog(newTripLog, null, null);
                    tripLogList.add(addedTripLog);
                    adapter.notifyDataSetChanged();
                    editTripLog(addedTripLog.getIdTrip());
                }
            }
        });

        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            dateToHighlight = (Date) extras.get("dateToHighlight");
        }
        userAuth = getUserInfo();
        getAllTripLog();
//        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    private void getAllTripLog() {
        UserAuth auth = getUserInfo();
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put(USERID, auth.getData().getUserId());
        hashMap.put(OTHER_USERID, auth.getData().getUserId());
        hashMap.put(IBOBBER_APP, "1");
        ApiLoader.getInstance().getResponse(this, hashMap, RestConstants.GET_CATCH, CatchTripListMain.class, new CallBack() {
            @Override
            public void onResponse(Call call, Response response, String msg, Object object) {
                listMain = ((CatchTripListMain) object);
                if (listMain != null && listMain.getData() != null)
                    TripLogService.getInstance(getApplicationContext()).saveTripLog(listMain.getData(), new TaskListener() {
                        @Override
                        public void onTaskCompleted() {
                            progressBar.setVisibility(View.GONE);
                            tripLogList = TripLogService.getInstance(getApplicationContext()).getCatchTripLog();
                            adapter = new TripLogAdapter(TripLogListActivity.this, tripLogList);
                            lstTrip.setAdapter(adapter);
                        }
                    });
            }

            @Override
            public void onFail(Call call, Throwable e) {
                findViewById(R.id.loadingSpinner).setVisibility(View.GONE);
                AppUtils.showToast(TripLogListActivity.this, getString(R.string.err_network),false);
            }

            @Override
            public void onSocketTimeout(Call call, Throwable e) {
                findViewById(R.id.loadingSpinner).setVisibility(View.GONE);
                AppUtils.showToast(TripLogListActivity.this, getString(R.string.err_network),false);
            }
        });
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.triplog_context_menu, menu);
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
        if (adapter.getItem(position).getTitle().equalsIgnoreCase(RestConstants.NETFISH_CATCH_TITLE)) {
            if (AppUtils.isAppInstalled(getApplicationContext(), NETFISH_PACKAGE)) {
                try {
                    Intent intent = new Intent(NETFISH_PACKAGE + NETFISH_CATCH_LOG_ACTIVITY);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(ISIBOBBER, true);
                    intent.putExtra(USERID, userAuth.getData().getUserId());
                    intent.putExtra(CATCH_ID, String.valueOf(adapter.getItem(position).getNetFishId()));
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    AppUtils.openAppOnPlayStore(getApplicationContext(), NETFISH_PACKAGE);
                }
            } else {
                AppUtils.openAppOnPlayStore(getApplicationContext(), NETFISH_PACKAGE);
            }
        } else {
            editTripLog(position);
        }


    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.edit:
                editTripLog(info.position);
                return true;
            case R.id.delete:
                deleteTripLog(info.position);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    public void editTripLog(final int position) {
        TripLog tripLog = (TripLog) adapter.getItem(position);
        long idTrip = tripLog.getIdTrip();
        Intent tripDetail = new Intent(this, TripLogDetailActivity.class);
        tripDetail.putExtra("idTrip", idTrip);
        startActivity(tripDetail);
    }


    public void editTripLog(final long tripLogId) {
        Intent tripDetail = new Intent(this, TripLogDetailActivity.class);
        tripDetail.putExtra("idTrip", tripLogId);
        startActivity(tripDetail);
    }

    public void deleteTripLog(final int position) {
        final TripLog tripLog = (TripLog) adapter.getItem(position);

        TripLogService.getInstance(TripLogListActivity.this).deleteTripLog(tripLog);
        adapter.removeItem(position);
    }

    public void onEventMainThread(final DBLoader.ContentChangedEvent event) {
//        findViewById(R.id.loadingSpinner).setVisibility(View.VISIBLE);
    }

}
