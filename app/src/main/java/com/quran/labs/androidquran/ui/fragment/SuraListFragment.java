package com.quran.labs.androidquran.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.quran.labs.androidquran.R;
import com.quran.labs.androidquran.dao.RecentPage;
import com.quran.labs.androidquran.data.QuranInfo;
import com.quran.labs.androidquran.ui.QuranActivity;
import com.quran.labs.androidquran.ui.helpers.QuranListAdapter;
import com.quran.labs.androidquran.ui.helpers.QuranRow;
import com.quran.labs.androidquran.util.QuranSettings;
import com.quran.labs.androidquran.util.QuranUtils;

import java.util.List;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

import static com.quran.labs.androidquran.data.Constants.JUZ2_COUNT;
import static com.quran.labs.androidquran.data.Constants.PAGES_LAST;
import static com.quran.labs.androidquran.data.Constants.SURAS_COUNT;

public class SuraListFragment extends Fragment {

  private RecyclerView mRecyclerView;
  private Subscription subscription;

  public static SuraListFragment newInstance() {
    return new SuraListFragment();
  }

  @Override
  public View onCreateView(LayoutInflater inflater,
      ViewGroup container, Bundle savedInstanceState) {
    final View view = inflater.inflate(R.layout.quran_list, container, false);

    final Context context = getActivity();
    mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
    mRecyclerView.setHasFixedSize(true);
    mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
    mRecyclerView.setItemAnimator(new DefaultItemAnimator());

    final QuranListAdapter adapter =
        new QuranListAdapter(context, mRecyclerView, getSuraList(), false);
    mRecyclerView.setAdapter(adapter);
    return view;
  }

  @Override
  public void onPause() {
    if (subscription != null) {
      subscription.unsubscribe();
    }
    super.onPause();
  }

  @Override
  public void onResume() {
    final Activity activity = getActivity();
    QuranSettings settings = QuranSettings.getInstance(activity);
    if (activity instanceof QuranActivity) {
      subscription = ((QuranActivity) activity).getRecentPagesObservable()
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(new Action1<List<RecentPage>>() {
            @Override
            public void call(List<RecentPage> recentPages) {
              if (recentPages.size() > 0) {
                int lastPage = recentPages.get(0).page;
                int sura = QuranInfo.PAGE_SURA_START[lastPage - 1];
                int juz = QuranInfo.getJuzFromPage(lastPage);
                int position = sura + juz - 1;
                mRecyclerView.scrollToPosition(position);
              }
              subscription = null;
            }
          });
    }

    if (settings.isArabicNames()) {
      updateScrollBarPositionHoneycomb();
    }

    super.onResume();
  }

  private void updateScrollBarPositionHoneycomb() {
    mRecyclerView.setVerticalScrollbarPosition(View.SCROLLBAR_POSITION_LEFT);
  }

  private QuranRow[] getSuraList() {
    int next;
    int pos = 0;
    int sura = 1;
    QuranRow[] elements = new QuranRow[SURAS_COUNT + JUZ2_COUNT];

    Activity activity = getActivity();
    boolean wantPrefix = activity.getResources().getBoolean(R.bool.show_surat_prefix);
    boolean wantTranslation = activity.getResources().getBoolean(R.bool.show_sura_names_translation);
    for (int juz = 1; juz <= JUZ2_COUNT; juz++) {
      final String headerTitle = activity.getString(R.string.juz2_description,
          QuranUtils.getLocalizedNumber(activity, juz));
      final QuranRow.Builder headerBuilder = new QuranRow.Builder()
          .withType(QuranRow.HEADER)
          .withText(headerTitle)
          .withPage(QuranInfo.JUZ_PAGE_START[juz - 1]);
      elements[pos++] = headerBuilder.build();
      next = (juz == JUZ2_COUNT) ? PAGES_LAST + 1 :
          QuranInfo.JUZ_PAGE_START[juz];

      while ((sura <= SURAS_COUNT) &&
          (QuranInfo.SURA_PAGE_START[sura - 1] < next)) {
        final QuranRow.Builder builder = new QuranRow.Builder()
            .withText(QuranInfo.getSuraName(activity, sura, wantPrefix, wantTranslation))
            .withMetadata(QuranInfo.getSuraListMetaString(activity, sura))
            .withSura(sura)
            .withPage(QuranInfo.SURA_PAGE_START[sura - 1]);
        elements[pos++] = builder.build();
        sura++;
      }
    }

    return elements;
  }
}
