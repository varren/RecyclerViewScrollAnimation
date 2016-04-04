package com.varren.animationdemo;
import android.support.v4.animation.AnimatorCompatHelper;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import java.util.ArrayList;
import java.util.List;

public class DefaultItemAnimator extends SimpleItemAnimator {
    private static final String TAG = "DefaultItemAnimator";

    private ArrayList<RecyclerView.ViewHolder> mPendingRemovals = new ArrayList<>();
    private ArrayList<RecyclerView.ViewHolder> mPendingAdditions = new ArrayList<>();
    private ArrayList<ArrayList<RecyclerView.ViewHolder>> mAdditionsList = new ArrayList<>();
    private ArrayList<RecyclerView.ViewHolder> mAddAnimations = new ArrayList<>();
    private ArrayList<RecyclerView.ViewHolder> mRemoveAnimations = new ArrayList<>();

    private static final int ANIMATION_DURATION = 400;

    @Override
    public void runPendingAnimations() {
        boolean removalsPending = !mPendingRemovals.isEmpty();
        boolean additionsPending = !mPendingAdditions.isEmpty();

        if (!removalsPending && !additionsPending) {
            // nothing to animate
            return;
        }

        // First, remove stuff
        for (RecyclerView.ViewHolder holder : mPendingRemovals) {
            animateRemoveImpl(holder);
        }
        mPendingRemovals.clear();

        // Next, add stuff
        if (additionsPending) {
            final ArrayList<RecyclerView.ViewHolder> additions = new ArrayList<>();
            additions.addAll(mPendingAdditions);
            mAdditionsList.add(additions);
            mPendingAdditions.clear();
            Runnable adder = new Runnable() {
                public void run() {
                    for (RecyclerView.ViewHolder holder : additions) {
                        animateAddImpl(holder);
                    }
                    additions.clear();
                    mAdditionsList.remove(additions);
                }
            };
            View view = additions.get(0).itemView;
            ViewCompat.postOnAnimationDelayed(view, adder, ANIMATION_DURATION);
        }
    }

    @Override
    public boolean animateRemove(final RecyclerView.ViewHolder holder) {
        resetAnimation(holder);
        mPendingRemovals.add(holder);
        return true;
    }

    @Override
    public boolean animateAdd(final RecyclerView.ViewHolder holder) {
        resetAnimation(holder);
        ViewCompat.setAlpha(holder.itemView, 0);
        mPendingAdditions.add(holder);
        return true;
    }

    @Override
    public boolean animateMove(final RecyclerView.ViewHolder holder, int fromX, int fromY,
                               int toX, int toY) {
        return animateAdd(holder);
    }

    @Override
    public boolean animateChange(RecyclerView.ViewHolder oldHolder, RecyclerView.ViewHolder newHolder,
                                 int fromX, int fromY, int toX, int toY) {
        animateAdd(newHolder);
        if (oldHolder != newHolder)
            animateRemove(oldHolder);
        return true;
    }

    private void animateRemoveImpl(final RecyclerView.ViewHolder holder) {
        Log.e(TAG, "animateRemoveImpl" + holder.getAdapterPosition());
        animateToLeft(holder);
    }

    private void animateAddImpl(final RecyclerView.ViewHolder newHolder) {
        Log.e(TAG, "animateAddImpl " + newHolder.getAdapterPosition());
        animateDownToUp(newHolder);
    }

    private AccelerateInterpolator interpolator = new AccelerateInterpolator();

    private void animateToLeft(final RecyclerView.ViewHolder oldHolder) {
        final View view = oldHolder != null ? oldHolder.itemView : null;
        if (view != null) {
            mRemoveAnimations.add(oldHolder);

            final ViewPropertyAnimatorCompat animOut = ViewCompat.animate(view)
                    .setDuration(ANIMATION_DURATION)
                    .setInterpolator(interpolator)
                    .translationX(view.getRootView().getWidth() + 1)
                    .alpha(0);

            animOut.setListener(new VpaListenerAdapter() {
                @Override
                public void onAnimationStart(View view) {
                    dispatchRemoveStarting(oldHolder);
                }

                @Override
                public void onAnimationEnd(View view) {
                    animOut.setListener(null);
                    ViewCompat.setAlpha(view, 1);
                    ViewCompat.setTranslationX(view, 0);
                    dispatchRemoveFinished(oldHolder);
                    mRemoveAnimations.remove(oldHolder);

                    dispatchFinishedWhenDone();
                }
            }).start();
        }
    }

    private void animateDownToUp(final RecyclerView.ViewHolder newHolder) {
        final View newView = newHolder != null ? newHolder.itemView : null;
        if (newView != null) {
            // setting starting animation params for view
            ViewCompat.setTranslationY(newView, 200);
            ViewCompat.setAlpha(newView, 0);

            mAddAnimations.add(newHolder);

            final ViewPropertyAnimatorCompat animIn = ViewCompat.animate(newView)
                    .setDuration(ANIMATION_DURATION)
                    .translationY(0)
                    .alpha(1);

            animIn.setListener(new VpaListenerAdapter() {
                @Override
                public void onAnimationStart(View view) {
                    dispatchAddStarting(newHolder);
                }

                @Override
                public void onAnimationEnd(View view) {
                    animIn.setListener(null);
                    ViewCompat.setAlpha(newView, 1);
                    ViewCompat.setTranslationY(newView, 0);
                    dispatchAddFinished(newHolder);
                    mAddAnimations.remove(newHolder);
                    dispatchFinishedWhenDone();
                }
            }).start();
        }
    }

    @Override
    public void endAnimation(RecyclerView.ViewHolder item) {
        final View view = item.itemView;
        // this will trigger end callback which should set properties to their target values.
        ViewCompat.animate(view).cancel();

        if (mPendingRemovals.remove(item)) {
            ViewCompat.setAlpha(view, 1);
            dispatchRemoveFinished(item);
        }
        if (mPendingAdditions.remove(item)) {
            ViewCompat.setAlpha(view, 1);
            dispatchAddFinished(item);
        }

        for (int i = mAdditionsList.size() - 1; i >= 0; i--) {
            ArrayList<RecyclerView.ViewHolder> additions = mAdditionsList.get(i);
            if (additions.remove(item)) {
                ViewCompat.setAlpha(view, 1);
                dispatchAddFinished(item);
                if (additions.isEmpty()) {
                    mAdditionsList.remove(i);
                }
            }
        }

        dispatchFinishedWhenDone();
    }

    private void resetAnimation(RecyclerView.ViewHolder holder) {
        AnimatorCompatHelper.clearInterpolator(holder.itemView);
        endAnimation(holder);
    }

    @Override
    public boolean isRunning() {
        return (!mPendingAdditions.isEmpty() ||
                !mPendingRemovals.isEmpty() ||
                !mRemoveAnimations.isEmpty() ||
                !mAddAnimations.isEmpty() ||
                !mAdditionsList.isEmpty());
    }

    private void dispatchFinishedWhenDone() {
        if (!isRunning()) {
            dispatchAnimationsFinished();
        }
    }

    @Override
    public void endAnimations() {
        int count;
        count = mPendingRemovals.size();
        for (int i = count - 1; i >= 0; i--) {
            RecyclerView.ViewHolder item = mPendingRemovals.get(i);
            dispatchRemoveFinished(item);
            mPendingRemovals.remove(i);
        }
        count = mPendingAdditions.size();
        for (int i = count - 1; i >= 0; i--) {
            RecyclerView.ViewHolder item = mPendingAdditions.get(i);
            View view = item.itemView;
            ViewCompat.setAlpha(view, 1);
            dispatchAddFinished(item);
            mPendingAdditions.remove(i);
        }

        if (!isRunning()) {
            return;
        }

        int listCount;
        listCount = mAdditionsList.size();
        for (int i = listCount - 1; i >= 0; i--) {
            ArrayList<RecyclerView.ViewHolder> additions = mAdditionsList.get(i);
            count = additions.size();
            for (int j = count - 1; j >= 0; j--) {
                RecyclerView.ViewHolder item = additions.get(j);
                View view = item.itemView;
                ViewCompat.setAlpha(view, 1);
                dispatchAddFinished(item);
                additions.remove(j);
                if (additions.isEmpty()) {
                    mAdditionsList.remove(additions);
                }
            }
        }


        cancelAll(mRemoveAnimations);
        cancelAll(mAddAnimations);

        dispatchAnimationsFinished();
    }

    void cancelAll(List<RecyclerView.ViewHolder> viewHolders) {
        for (int i = viewHolders.size() - 1; i >= 0; i--) {
            ViewCompat.animate(viewHolders.get(i).itemView).cancel();
        }
    }

    private static class VpaListenerAdapter implements ViewPropertyAnimatorListener {
        @Override
        public void onAnimationStart(View view) {
        }

        @Override
        public void onAnimationEnd(View view) {
        }

        @Override
        public void onAnimationCancel(View view) {
        }
    }
}
