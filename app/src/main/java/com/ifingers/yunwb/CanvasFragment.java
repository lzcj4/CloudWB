package com.ifingers.yunwb;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.ifingers.yunwb.utility.WhiteboardTaskContext;

public class CanvasFragment extends Fragment {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private SurfaceView surface;
    private View view;
    private OnFragmentInteractionListener mListener;

    public CanvasFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if (view == null)
            view = inflater.inflate(R.layout.fragment_canvas, container, false);

        ViewTreeObserver vto = view.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                //create surface when size is determined
                if (surface == null) {
                    surface = (SurfaceView) getActivity().findViewById(R.id.surfaceView);
                    surface.getHolder().setFormat(PixelFormat.RGBA_8888);
                    surface.setZOrderOnTop(true);
                    surface.setBackgroundColor(Color.WHITE);
                    surface.getHolder().addCallback((SurfaceHolder.Callback) getActivity());
                    scale(view.getWidth(), view.getHeight());
                }
            }
        });
        return view;
    }

    private void scale(int parentWidth, int parentHeigth) {
        //adjust surface size according to hardware size
        int defaultHeight = parentHeigth - 20;//designed margin
        int defaultWidth = parentWidth - 40;//designed margin
        float defaultWHRatio = defaultWidth / (float)defaultHeight;
        float designedWHRatio = WhiteboardTaskContext.getInstance().getHardwareWHRatio();
        DisplayMetrics dm = new DisplayMetrics();
        this.getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);

        if (defaultWHRatio >= designedWHRatio) {
            //keep height and adjust width
            int designedWidth = (int)(designedWHRatio * defaultHeight);
            ViewGroup.LayoutParams lp = surface.getLayoutParams();
            lp.width = designedWidth;
            lp.height = defaultHeight;
            surface.setLayoutParams(lp);
            surface.getHolder().setFixedSize(lp.width, lp.height);
        } else {
            //keep width and adjust height
            int designedHeight = (int)(defaultWidth / designedWHRatio);
            ViewGroup.LayoutParams lp = surface.getLayoutParams();
            lp.width = defaultWidth;
            lp.height = designedHeight;
            surface.setLayoutParams(lp);
            surface.getHolder().setFixedSize(lp.width, lp.height);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    public SurfaceView getSurfaceView() {
        return surface;
    }
}
