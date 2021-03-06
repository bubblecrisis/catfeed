//
// DO NOT EDIT THIS FILE, IT HAS BEEN GENERATED USING AndroidAnnotations.
//


package com.catfeed.activity;

import java.io.Serializable;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;
import com.catfeed.CatFeedApp;
import com.catfeed.R.id;
import com.catfeed.R.layout;
import com.catfeed.RssFeeder_;
import com.catfeed.db.Repository_;

public final class WebSummaryActivity_
    extends WebSummaryActivity
{


    @Override
    public void onCreate(Bundle savedInstanceState) {
        init_(savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(layout.websummary);
    }

    private void init_(Bundle savedInstanceState) {
        injectExtras_();
        application = ((CatFeedApp) this.getApplication());
        rss = RssFeeder_.getInstance_(this);
        repository = Repository_.getInstance_(this);
    }

    private void afterSetContentView_() {
        title = ((TextView) findViewById(id.title));
        ((RssFeeder_) rss).afterSetContentView_();
        ((Repository_) repository).afterSetContentView_();
        prepopulate();
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        afterSetContentView_();
    }

    @Override
    public void setContentView(View view, LayoutParams params) {
        super.setContentView(view, params);
        afterSetContentView_();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        afterSetContentView_();
    }

    public static WebSummaryActivity_.IntentBuilder_ intent(Context context) {
        return new WebSummaryActivity_.IntentBuilder_(context);
    }

    @SuppressWarnings("unchecked")
    private<T >T cast_(Object object) {
        return ((T) object);
    }

    private void injectExtras_() {
        Intent intent_ = getIntent();
        Bundle extras_ = intent_.getExtras();
        if (extras_!= null) {
            if (extras_.containsKey("feedId")) {
                try {
                    feedId = cast_(extras_.get("feedId"));
                } catch (ClassCastException e) {
                    Log.e("WebSummaryActivity_", "Could not cast extra to expected type, the field is left to its default value", e);
                }
            }
            if (extras_.containsKey("subscriptionTitle")) {
                try {
                    subscriptionTitle = cast_(extras_.get("subscriptionTitle"));
                } catch (ClassCastException e) {
                    Log.e("WebSummaryActivity_", "Could not cast extra to expected type, the field is left to its default value", e);
                }
            }
        }
    }

    @Override
    public void setIntent(Intent newIntent) {
        super.setIntent(newIntent);
        injectExtras_();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(com.catfeed.R.menu.websummary, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean handled = super.onOptionsItemSelected(item);
        if (handled) {
            return true;
        }
        int itemId_ = item.getItemId();
        if (itemId_ == id.menuitem_search) {
            searchMenuClicked();
            return true;
        }
        return false;
    }

    public static class IntentBuilder_ {

        private Context context_;
        private final Intent intent_;

        public IntentBuilder_(Context context) {
            context_ = context;
            intent_ = new Intent(context, WebSummaryActivity_.class);
        }

        public Intent get() {
            return intent_;
        }

        public WebSummaryActivity_.IntentBuilder_ flags(int flags) {
            intent_.setFlags(flags);
            return this;
        }

        public void start() {
            context_.startActivity(intent_);
        }

        public void startForResult(int requestCode) {
            if (context_ instanceof Activity) {
                ((Activity) context_).startActivityForResult(intent_, requestCode);
            } else {
                context_.startActivity(intent_);
            }
        }

        public WebSummaryActivity_.IntentBuilder_ feedId(Long feedId) {
            intent_.putExtra("feedId", ((Serializable) feedId));
            return this;
        }

        public WebSummaryActivity_.IntentBuilder_ subscriptionTitle(String subscriptionTitle) {
            intent_.putExtra("subscriptionTitle", subscriptionTitle);
            return this;
        }

    }

}
