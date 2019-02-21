package com.example.administrator.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

//import butterknife.BindView;
//import butterknife.ButterKnife;
//import butterknife.OnClick;

public class MainActivity extends Activity implements View.OnClickListener {


    Button testRegist;
    Button testPost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        testRegist = findViewById(R.id.test_regist);
        testPost = findViewById(R.id.test_post);

        testPost.setOnClickListener(this);
        testRegist.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.test_regist:
                startActivity(new Intent(this,EventBusRegistActivity.class));
                break;
            case R.id.test_post:
                new TestPostEvent().post();
                break;
        }
    }


//    @OnClick({R.id.test_regist, R.id.test_post})
//    public void onViewClicked(View view) {
//        switch (view.getId()) {
//            case R.id.test_regist:
//                startActivity(new Intent(this,EventBusRegistActivity.class));
//                break;
//            case R.id.test_post:
//                break;
//        }
//    }
}
