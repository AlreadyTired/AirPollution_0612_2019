package com.pce_mason.qi.airpollution.UserManagements;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.google.android.gms.plus.model.people.Person;
import com.pce_mason.qi.airpollution.R;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class FragmentSguFirst extends Fragment implements SignUpActivity.onKeyBackPressedListener{
    String mId;
    private Button Continue_btn;
    private EditText FirstNameEdt, LastNameEdt, BirthDateEdt;
    private TextInputLayout FirstNameLayout, LastNameLayout, BirthDateLayout, RadioGroupLayout;
    private RadioGroup GenderSelector;
    private String Gender = "Male";
    private RadioButton MaleBtn, FemaleBtn;


    public FragmentSguFirst() {
        // Required empty public constructor
    }
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        Log.d(this.getClass().getSimpleName(), "onCreate()");
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if(args != null)
        {
            mId = args.getString("id","");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sgu_first, container, false);
        Log.d(this.getClass().getSimpleName(), "onCreateView()");

        FirstNameEdt = (EditText)view.findViewById(R.id.signUpFirstName);
        LastNameEdt = (EditText)view.findViewById(R.id.signUpLastName);
        BirthDateEdt = (EditText)view.findViewById(R.id.signUpBirth);
        GenderSelector = (RadioGroup)view.findViewById(R.id.SignUpRadioGroup);
        FirstNameLayout = (TextInputLayout)view.findViewById(R.id.SignUpFirstNameLayout);
        LastNameLayout = (TextInputLayout)view.findViewById(R.id.SignUpLastNameLayout);
        BirthDateLayout = (TextInputLayout)view.findViewById(R.id.SignUpBirthLayout);
        RadioGroupLayout = (TextInputLayout)view.findViewById(R.id.radioGroupLayout);
        MaleBtn = (RadioButton)view.findViewById(R.id.SignUpGenderMale);
        FemaleBtn = (RadioButton)view.findViewById(R.id.SignUpGenderFemale);

        //RadioGroup
        GenderSelector.setOnCheckedChangeListener(radioGroupButtonChangeListener);

        // Continue Button
        Continue_btn = (Button)view.findViewById(R.id.sgu_firstpage_continue_btn);
        Continue_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptNextStep();
            }
        });

        // Birthdate calender
        BirthDateEdt.setText(getCurrentDate());
        BirthDateEdt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(v);
                hideKeyboard(getActivity());
            }
        });
        BirthDateEdt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean focusOn) {
                if (focusOn){
                  showDatePickerDialog(view);
                  hideKeyboard(getActivity());
                }
            }
        });

        FirstNameEdt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus)
                {
                    setFocusedEdtBackground(FirstNameEdt);
                }
                if(!hasFocus)
                {
                    setEnableEdtBackground(FirstNameEdt);
                }
            }
        });
        LastNameEdt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus)
                {
                    setFocusedEdtBackground(LastNameEdt);
                }
                if(!hasFocus)
                {
                    setEnableEdtBackground(LastNameEdt);
                }
            }
        });
        BirthDateEdt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus)
                {
                    setFocusedEdtBackground(BirthDateEdt);
                }
                if(!hasFocus)
                {
                    setEnableEdtBackground(BirthDateEdt);
                }
            }
        });

        // Top page status bar change
        ((SignUpActivity)getActivity()).SGUStatusBarChange(1);

        // Inflate the layout for this fragment
        return view;
    }

    //RadioGroup
    RadioGroup.OnCheckedChangeListener radioGroupButtonChangeListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            if(checkedId == R.id.SignUpGenderMale)
            {
                Gender = "Male";
            }
            else if(checkedId == R.id.SignUpGenderFemale)
            {
                Gender = "Female";
            }
            hideKeyboard(getActivity());
        }
    };

    @Override
    public void onBackKey()
    {
        SignUpActivity SguActivity = (SignUpActivity)getActivity();
        SguActivity.setOnKeyBackPressedListener(null);
        SguActivity.onBackPressed();
    }

    @Override
    public void onAttach(Context context)
    {
        Log.d(this.getClass().getSimpleName(), "onAttach()");
        super.onAttach(context);
        ((SignUpActivity)context).setOnKeyBackPressedListener(this);
    }



    public void attemptNextStep()
    {
        //error reset
        FirstNameLayout.setError(null);
        LastNameLayout.setError(null);
        BirthDateLayout.setError(null);
        RadioGroupLayout.setError(null);
        setEnableEdtBackground(FirstNameEdt);
        setEnableEdtBackground(LastNameEdt);
        setEnableEdtBackground(BirthDateEdt);

        String LastName = LastNameEdt.getText().toString();
        String FirstName = FirstNameEdt.getText().toString();
        String BirthDate = BirthDateEdt.getText().toString();

        boolean cancel = false;
        View focusView = null;
        hideKeyboard(getActivity());
        // Check for a valid first name
        if (TextUtils.isEmpty(FirstName)) {
            FirstNameLayout.setErrorTextAppearance(R.style.errorcolor);
            FirstNameLayout.setError(getString(R.string.sgu_name_valid_error));
            setErrorEdtBackground(FirstNameEdt);
            focusView = FirstNameEdt;
            cancel = true;
        }
        // Check for a valid last name
        if (TextUtils.isEmpty(LastName)) {
            LastNameLayout.setErrorTextAppearance(R.style.errorcolor);
            LastNameLayout.setError(getString(R.string.sgu_name_valid_error));
            setErrorEdtBackground(LastNameEdt);
            focusView = LastNameEdt;
            cancel = true;
        }
        // Check for a valid birthDate
        if (TextUtils.isEmpty(BirthDate)) {
            BirthDateLayout.setErrorTextAppearance(R.style.errorcolor);
            BirthDateLayout.setError(getString(R.string.birth_valid_error));
            setErrorEdtBackground(BirthDateEdt);
            focusView = BirthDateEdt;
            cancel = true;
        }
        // Check for a valid gender
        if(!MaleBtn.isChecked() && !FemaleBtn.isChecked())
        {
            RadioGroupLayout.setErrorTextAppearance(R.style.errorcolor);
            RadioGroupLayout.setError("Select one of those");
            focusView = RadioGroupLayout;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
            if(focusView != RadioGroupLayout) { setErrorEdtBackground((EditText)focusView); }
        } else {
            ((SignUpActivity)getActivity()).replaceFragment(2,FragmentSguSecond.getInstance(FirstName,LastName,BirthDate,Gender));
        }

    }

    private void hideKeyboard(Activity activity) {
        View view = activity.findViewById(android.R.id.content);
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public void showDatePickerDialog(View v) {
        DialogFragment newFragment = DatePickerFragment.newInstance(1);
        newFragment.show(getFragmentManager(), "datePicker");
    }
    protected String getCurrentDate(){
        Date today = Calendar.getInstance().getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
        return formatter.format(today);
    }

    public void setEnableEdtBackground(EditText Edt)
    {
        Edt.setBackgroundResource(R.drawable.edittext_border_enable);
        Edt.setTextColor(Color.parseColor(getString(R.string.Normal_Color)));
    }

    public void setFocusedEdtBackground(EditText Edt)
    {
        Edt.setBackgroundResource(R.drawable.edittext_border_focused);
    }

    public void setErrorEdtBackground(EditText Edt)
    {
        Edt.setBackgroundResource(R.drawable.edittext_border_error);
        Edt.setTextColor(Color.parseColor(getString(R.string.Error_Color)));
    }
}
