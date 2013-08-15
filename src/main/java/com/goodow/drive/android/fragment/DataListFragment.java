package com.goodow.drive.android.fragment;

import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.goodow.android.drive.R;
import com.goodow.drive.android.Interface.INotifyData;
import com.goodow.drive.android.Interface.IRemoteControl;
import com.goodow.drive.android.Interface.ILocalFragment;
import com.goodow.drive.android.activity.MainActivity;
import com.goodow.drive.android.adapter.CollaborativeAdapter;
import com.goodow.drive.android.adapter.CollaborativeAdapter.OnItemClickListener;
import com.goodow.drive.android.global_data_cache.GlobalConstant;
import com.goodow.drive.android.global_data_cache.GlobalDataCacheForMemorySingleton;
import com.goodow.realtime.BaseModelEvent;
import com.goodow.realtime.CollaborativeList;
import com.goodow.realtime.CollaborativeMap;
import com.goodow.realtime.Document;
import com.goodow.realtime.DocumentLoadedHandler;
import com.goodow.realtime.EventHandler;
import com.goodow.realtime.Model;
import com.goodow.realtime.ModelInitializerHandler;
import com.goodow.realtime.ObjectChangedEvent;
import com.goodow.realtime.Realtime;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

public class DataListFragment extends ListFragment implements ILocalFragment {
  private final String TAG = this.getClass().getSimpleName();

  private IRemoteControl path;
  private JsonArray currentPathList;
  private CollaborativeMap currentFolder;

  private CollaborativeAdapter adapter;

  private Document doc;
  private Model model;
  private CollaborativeMap root;

  private String DOCID;
  private static final String FOLDER_KEY = GlobalConstant.DocumentIdAndDataKey.FOLDERSKEY.getValue();
  private static final String FILE_KEY = GlobalConstant.DocumentIdAndDataKey.FILESKEY.getValue();

  private EventHandler<?> listEventHandler;
  private EventHandler<ObjectChangedEvent> valuesChangeEventHandler;
  private INotifyData iNotifyData;

  public void backFragment() {
    if (null != currentPathList && 1 < currentPathList.length()) {
      String mapId = currentPathList.get(currentPathList.length() - 1).asString();
      CollaborativeMap currentmap = model.getObject(mapId);
      if (null != currentmap) {
        // 删除监听
        currentmap.removeObjectChangedListener(valuesChangeEventHandler);
      } else {
        Toast.makeText(getActivity(), R.string.remoteControlError, Toast.LENGTH_SHORT).show();
      }

      path.changePath(null, DOCID);
    } else {
      if (null != getActivity()) {
        Toast.makeText(getActivity(), R.string.backFolderErro, Toast.LENGTH_SHORT).show();
      }
    }
  }

  public void connectUi() {
    Log.i(TAG, "connectUi()");

    MainActivity activity = (MainActivity) getActivity();
    if (null != activity) {
      path = activity.getRemoteControlObserver();
    }

    if (null != path) {
      path.setNotifyData(iNotifyData);

      currentPathList = path.getCurrentPath();
      if (0 == currentPathList.length()) {
        path.changePath(root.getId(), DOCID);
        currentPathList = path.getCurrentPath();
      }

      showData();
    }
  }

  public void showData() {
    if (null != getActivity()) {
      RelativeLayout relativeLayout = (RelativeLayout) getActivity().findViewById(R.id.mainConnect);
      relativeLayout.setVisibility(View.GONE);
    }

    currentFolder = model.getObject(currentPathList.get(currentPathList.length() - 1).asString());

    if (null != currentFolder) {
      currentFolder.addObjectChangedListener(valuesChangeEventHandler);
      CollaborativeList folderList = (CollaborativeList) currentFolder.get(FOLDER_KEY);
      CollaborativeList fileList = (CollaborativeList) currentFolder.get(FILE_KEY);

      adapter.setFolderList(folderList);
      adapter.setFileList(fileList);
      adapter.notifyDataSetChanged();

      // 设置action bar的显示
      MainActivity activity = (MainActivity) getActivity();
      if (null != activity) {
        if (currentPathList.length() <= 1) {
          activity.setActionBarTitle(GlobalConstant.MenuTypeEnum.USER_REMOTE_DATA.getMenuName());
        } else {
          StringBuffer title = new StringBuffer();
          for (int i = 0; i < currentPathList.length(); i++) {
            CollaborativeMap currentMap = model.getObject(currentPathList.get(i).asString());

            if (null != currentMap) {
              String label = currentMap.get("label");
              if (null != label) {
                title.append(label + "/");
              }
            } else {
              Toast.makeText(getActivity(), R.string.remoteControlError, Toast.LENGTH_SHORT).show();
            }
          }

          activity.setActionBarTitle(title.toString());
        }
      }
    } else {
      Toast.makeText(getActivity(), R.string.remoteControlError, Toast.LENGTH_SHORT).show();
    }

    openState();
  }

  @Override
  public void onPause() {
    super.onPause();

    ((MainActivity) getActivity()).restActionBarTitle();
  }

  @Override
  public void onResume() {
    super.onResume();
    MainActivity activity = (MainActivity) getActivity();

    activity.setLocalFragment(this);
    activity.setLastiRemoteDataFragment(this);

    loadDocument();
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    Log.i(TAG, "onActivityCreated()");

    MainActivity activity = (MainActivity) getActivity();

    activity.setActionBarTitle("我的收藏夹");
    TextView textView = (TextView) activity.findViewById(R.id.openfailure_text);
    ImageView imageView = (ImageView) activity.findViewById(R.id.openfailure_img);
    activity.setOpenStateView(textView, imageView);
  }

  private void initEventHandler() {
    if (listEventHandler == null) {
      listEventHandler = new EventHandler<BaseModelEvent>() {
        @Override
        public void handleEvent(BaseModelEvent event) {
          adapter.notifyDataSetChanged();

          openState();
        }
      };
    }

    if (valuesChangeEventHandler == null) {
      valuesChangeEventHandler = new EventHandler<ObjectChangedEvent>() {
        @Override
        public void handleEvent(ObjectChangedEvent event) {
          adapter.notifyDataSetChanged();

          openState();
        }
      };
    }

    if (iNotifyData == null) {
      iNotifyData = new INotifyData() {
        @Override
        public void notifyData(JsonObject newJson) {
          Log.i(TAG, "notifyData()");

          currentPathList = newJson.get(GlobalConstant.DocumentIdAndDataKey.CURRENTPATHKEY.getValue());

          if (null != currentPathList && 0 != currentPathList.length()) {
            CollaborativeMap map = model.getObject(currentPathList.get(currentPathList.length() - 1).asString());
            if (null != map) {
              showData();
            } else {
              Toast.makeText(getActivity(), R.string.remoteControlError, Toast.LENGTH_SHORT).show();
            }
          }
        }
      };
    }
  }

  private void openState() {
    if (null != currentFolder) {
      CollaborativeList folderList = currentFolder.get(FOLDER_KEY);
      CollaborativeList fileList = currentFolder.get(FILE_KEY);

      MainActivity activity = (MainActivity) getActivity();
      if (null != activity) {
        if (null != folderList && 0 == folderList.length() && null != fileList && 0 == fileList.length()) {
          activity.openState(LinearLayout.VISIBLE);
        } else {
          activity.openState(LinearLayout.INVISIBLE);
        }
      }
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.i(TAG, "onCreate()");

    RelativeLayout relativeLayout = (RelativeLayout) getActivity().findViewById(R.id.mainConnect);
    relativeLayout.setVisibility(View.VISIBLE);

    adapter = new CollaborativeAdapter(this.getActivity(), null, null, new OnItemClickListener() {
      @Override
      public void onItemClick(CollaborativeMap file) {
        MainActivity activity = (MainActivity) DataListFragment.this.getActivity();

        DataDetailFragment dataDetailFragment = activity.getDataDetailFragment();
        dataDetailFragment.setFile(file);
        dataDetailFragment.initView();

        activity.setDataDetailLayoutState(View.VISIBLE);
        activity.setLocalFragmentForDetail(dataDetailFragment);
      }
    });
    setListAdapter(adapter);

    initEventHandler();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    return inflater.inflate(R.layout.fragment_folderlist, container, false);
  }

  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    CollaborativeMap clickItem = (CollaborativeMap) v.getTag();

    if (null == clickItem.get("blobKey")) {
      path.changePath(clickItem.getId(), DOCID);
    } else {
      path.playFile(clickItem);
    }
  }

  @Override
  public void loadDocument() {
    DOCID = "@tmp/" + GlobalDataCacheForMemorySingleton.getInstance().getUserId() + "/" + GlobalConstant.DocumentIdAndDataKey.FAVORITESDOCID.getValue();
    Log.i(TAG, "loadDocument() DOCID: " + DOCID);

    // 文件Document
    DocumentLoadedHandler onLoaded = new DocumentLoadedHandler() {
      @Override
      public void onLoaded(Document document) {
        Log.i(TAG, "onLoaded()");

        doc = document;
        model = doc.getModel();
        root = model.getRoot();

        connectUi();
      }
    };

    ModelInitializerHandler initializer = new ModelInitializerHandler() {
      @Override
      public void onInitializer(Model model_) {
        model = model_;
        root = model.getRoot();

        String[] mapKey = { "label", FILE_KEY, FOLDER_KEY };
        CollaborativeMap[] values = new CollaborativeMap[3];

        for (int k = 0; k < values.length; k++) {
          CollaborativeMap map = model.createMap(null);
          for (int i = 0; i < mapKey.length; i++) {
            if ("label".equals(mapKey[i])) {

              map.set(mapKey[i], "Folder " + k);
            } else {
              CollaborativeList subList = model.createList();

              if (FOLDER_KEY.equals(mapKey[i])) {
                CollaborativeMap subMap = model.createMap(null);
                subMap.set("label", "SubFolder");
                subMap.set(FILE_KEY, model.createList());
                subMap.set(FOLDER_KEY, model.createList());
                subList.push(subMap);
              }

              map.set(mapKey[i], subList);
            }
          }

          values[k] = map;
        }

        CollaborativeList list = model_.createList();
        list.pushAll((Object[]) values);

        root.set(GlobalConstant.DocumentIdAndDataKey.FOLDERSKEY.getValue(), list);
      }
    };

    Realtime.load(DOCID, onLoaded, initializer, null);
  }
}
