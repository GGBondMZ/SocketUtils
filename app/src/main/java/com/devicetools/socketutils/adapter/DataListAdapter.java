package com.devicetools.socketutils.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.devicetools.socketutils.R;

import java.util.List;

/**
 * Created by mz on 2023/09/19.
 * Time: 09:10
 * Description: Server
 */

public class DataListAdapter extends BaseQuickAdapter<String, BaseViewHolder> {

    public DataListAdapter(List list) {
        super(R.layout.item_layout, list);
    }

    @Override
    protected void convert(BaseViewHolder helper, String item) {
        helper.setText(R.id.textView, item);
    }

    public void clean() {
        this.getData().clear();
        notifyDataSetChanged();
    }
}