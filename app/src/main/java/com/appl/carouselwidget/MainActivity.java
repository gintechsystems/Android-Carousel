package com.appl.carouselwidget;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.appl.library.CoverFlowCarousel;

public class MainActivity extends AppCompatActivity {

    CoverFlowCarousel carousel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final CarouselAdapter adapter = new CarouselAdapter();

        carousel = findViewById(R.id.carousel);
        carousel.setAdapter(adapter);
        carousel.setSelection(adapter.getCount() / 2);
        carousel.setSlowDownCoefficient(1);
        carousel.setSpacing(0.5f);
        carousel.setRotationThreshold(0.3f);
        //When not using repeat, I suggest replacing getCount() below = mCount along with getItem = mResourceIds[position % mResourceIds.length].
        // shouldRepeat is currently broken, clicks do not go to the correct position.
        //carousel.shouldRepeat(true);
    }


    private class CarouselAdapter extends BaseAdapter {
        private final int[] mResourceIds = {R.drawable.poster1, R.drawable.poster2, R.drawable.poster3, R.drawable.poster4 };
        //private int mCount = mResourceIds.length * 4;

        @Override
        public int getCount() {
            return mResourceIds.length;
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            MyFrame v;
            if (convertView == null) {
                v = new MyFrame(MainActivity.this);
            } else {
                v = (MyFrame)convertView;
            }

            v.setImageResource(mResourceIds[position]);
            v.setOnClickListener(v2 -> {
                carousel.scrollToItemPosition(position);
                Toast.makeText(MainActivity.this, "clicked position:" + position, Toast.LENGTH_SHORT).show();
            });

            return v;
        }

        public void addView() {
            //mCount++;
            notifyDataSetChanged();
        }
    }

    public static class MyFrame extends FrameLayout {
        private final ImageView mImageView;

        public void setImageResource(int resId){
            mImageView.setImageResource(resId);
        }

        public MyFrame(Context context) {
            super(context);

            mImageView = new ImageView(context);
            mImageView.setScaleType(ImageView.ScaleType.FIT_XY);
            addView(mImageView);

            setBackgroundColor(Color.WHITE);
            setSelected(false);
        }

        @Override
        public void setSelected(boolean selected) {
            super.setSelected(selected);

            if(selected) {
                mImageView.setAlpha(1.0f);
            } else {
                mImageView.setAlpha(0.5f);
            }
        }
    }
}
