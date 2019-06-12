package com.pce_mason.qi.airpollution.UserManagements;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.ImageView;
import android.widget.Toast;

import com.pce_mason.qi.airpollution.AppClientHeader.StateNumber;
import com.pce_mason.qi.airpollution.R;

import static com.pce_mason.qi.airpollution.MainActivity.APP_STATE;
import static com.pce_mason.qi.airpollution.MainActivity.StateCheck;

public class SignUpActivity extends AppCompatActivity{

    // UI references.
    private ImageView firstDivider, secondDivider, thirdDivider;
    Toolbar toolbar;

    Fragment Firstfragment,Secondfragment,Thirdfragment;
    FragmentTransaction fragmentTransaction;
    FragmentManager fragmentManager;


    // BackPress
    private BackPressCloseHandler backPressCloseHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        firstDivider = (ImageView)findViewById(R.id.sgu_divider_first);
        secondDivider = (ImageView)findViewById(R.id.sgu_divider_second);
        thirdDivider = (ImageView)findViewById(R.id.sgu_divider_third);

        // Backpressed
        backPressCloseHandler = new BackPressCloseHandler(this);

        // Fragment setting
        Firstfragment = new FragmentSguFirst();
        Secondfragment = new FragmentSguSecond();
        Thirdfragment = new FragmentSguThird_UVC();
        fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.sgu_frame_layout,Firstfragment,"FirstFragment").commit();
    }

    // Back pressed
    public interface onKeyBackPressedListener
    {
        void onBackKey();
    }

    private onKeyBackPressedListener mOnKeyBackPressedListener;
    public void setOnKeyBackPressedListener(onKeyBackPressedListener listener)
    {
        mOnKeyBackPressedListener = listener;
    }

    @Override
    public void onBackPressed() {
        if (mOnKeyBackPressedListener != null)
        {
            mOnKeyBackPressedListener.onBackKey();
            if(APP_STATE != StateNumber.STATE_SAP.IDLE_STATE) {
                APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                StateCheck("SGU_UVC");
            }
        }
        else
        {
            if(getSupportFragmentManager().getBackStackEntryCount() == 0)
            {
                backPressCloseHandler.onBackPressed();
            }
            else
            {
                super.onBackPressed();
            }
        }
    }

    public class BackPressCloseHandler {
        private long backKeyPressedTime = 0;
        private Toast toast;
        private Activity activity;
        public BackPressCloseHandler(Activity context)
        {
            this.activity = context;
        }
        public void onBackPressed() {
            if (System.currentTimeMillis() > backKeyPressedTime + 2000)
            {
                backKeyPressedTime = System.currentTimeMillis();
                showGuide();
                return;
            }
            if (System.currentTimeMillis() <= backKeyPressedTime + 2000)
            {
                activity.finish();
                toast.cancel();
            }
        }

        public  void showGuide() {
            toast = Toast.makeText(activity,"Press again to Main",Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    //Fragment replace
    public void replaceFragment(int index, Fragment Fragment)
    {
        fragmentManager = getSupportFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();
        if(index == 2)
        {
            fragmentTransaction.replace(R.id.sgu_frame_layout,Fragment,"SecondFragment");
        }
        else if(index == 3)
        {
            fragmentTransaction.replace(R.id.sgu_frame_layout,Fragment,"ThirdFragment");
        }
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    public void SGUStatusBarChange(int index)
    {
        if(index == 1)
        {
            firstDivider.setImageResource(R.drawable.fragment_page_divider_selected);
            secondDivider.setImageResource(R.drawable.fragment_page_divider);
            thirdDivider.setImageResource(R.drawable.fragment_page_divider);
        }
        else if(index == 2)
        {
            firstDivider.setImageResource(R.drawable.fragment_page_divider);
            secondDivider.setImageResource(R.drawable.fragment_page_divider_selected);
            thirdDivider.setImageResource(R.drawable.fragment_page_divider);
        }
        else if(index == 3)
        {
            firstDivider.setImageResource(R.drawable.fragment_page_divider);
            secondDivider.setImageResource(R.drawable.fragment_page_divider);
            thirdDivider.setImageResource(R.drawable.fragment_page_divider_selected);
        }
    }
}

