package com.genius.groupie;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * An adapter that holds a list of Groups.
 */
public class GroupAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Group.GroupDataObserver {
    private final View.OnClickListener onItemClickListener;
    private final List<Group> groups = new ArrayList<>();
    private int spanCount = 1;

    public GroupAdapter(View.OnClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    private final GridLayoutManager.SpanSizeLookup spanSizeLookup = new GridLayoutManager.SpanSizeLookup() {
        @Override public int getSpanSize(int position) {
            try {
                return getItem(position).getSpanSize(spanCount, position);
            } catch (IndexOutOfBoundsException e) {
                // Bug in support lib?  TODO investigate further
                return spanCount;
            }
        }
    };

    public GridLayoutManager.SpanSizeLookup getSpanSizeLookup() {
        return spanSizeLookup;
    }

    public void setSpanCount(int spanCount) {
        this.spanCount = spanCount;
    }

    public int getSpanCount() {
        return spanCount;
    }

    @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int layoutResId) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ViewDataBinding binding = DataBindingUtil.inflate(inflater, layoutResId, parent, false);
        return new ViewHolder<>(binding);
    }

    @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Item contentItem = getItem(position);
        contentItem.bind(holder, position, onItemClickListener);
    }

    @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position, List<Object> payloads) {
        Item contentItem = getItem(position);
        contentItem.bind(holder, position, payloads, onItemClickListener);
    }

    @Override public int getItemViewType(int position) {
        Item contentItem = getItem(position);
        if (contentItem == null) throw new RuntimeException("Invalid position " + position);
        return getItem(position).getLayout();
    }

    public Item getItem(int position) {
        int count = 0;
        for (Group group : groups) {
            if (position < count + group.getItemCount()) {
                return group.getItem(position - count);
            } else {
                count += group.getItemCount();
            }
        }
        throw new IndexOutOfBoundsException("Requested position " + position + "in group adapter " +
                "but there are only " + count + " items");
    }

    public int getAdapterPosition(Item contentItem) {
        int count = 0;
        for (Group group : groups) {
            int index = group.getPosition(contentItem);
            if (index >= 0) return index + count;
            count += group.getItemCount();
        }
        return -1;
    }

    /**
     * The position in the flat list of individual items at which the group starts
     *
     * @param group
     * @return
     */
    public int getAdapterPosition(Group group) {
        int index = groups.indexOf(group);
        int position = 0;
        for (int i = 0; i < index; i++) {
            position += groups.get(i).getItemCount();
        }
        return position;
    }

    @Override public int getItemCount() {
        int count = 0;
        for (Group group : groups) {
            count += group.getItemCount();
        }
        return count;
    }

    public int getItemCount(int groupIndex) {
        if (groupIndex >= groups.size()) {
            throw new IndexOutOfBoundsException("Requested group index " + groupIndex + " but there are " + groups.size() + " groups");
        }
        return groups.get(groupIndex).getItemCount();
    }

    public void clear() {
        for (Group group : groups) {
            group.setGroupDataObserver(null);
        }
        groups.clear();
        notifyDataSetChanged();
    }

    public void add(Group group) {
        int itemCountBeforeGroup = getItemCount();
        group.setGroupDataObserver(this);
        groups.add(group);
        notifyItemRangeInserted(itemCountBeforeGroup, group.getItemCount());
    }

    public void addAll(List<Group> groups) {
        int itemCountBeforeGroup = getItemCount();
        int additionalSize = 0;
        for (Group group : groups) {
            additionalSize += group.getItemCount();
            group.setGroupDataObserver(this);
        }
        this.groups.addAll(groups);
        notifyItemRangeInserted(itemCountBeforeGroup, additionalSize);
    }

    public void remove(Group group) {
        int position = groups.indexOf(group);
        int count = groups.get(position).getItemCount();
        group.setGroupDataObserver(null);
        groups.remove(position);
        notifyItemRangeRemoved(position, count);
    }

    public void putGroup(int index, Group group) {
        group.setGroupDataObserver(this);
        groups.add(index, group);
        int itemCountBeforeGroup = getItemCountBeforeGroup(index);
        notifyItemRangeInserted(itemCountBeforeGroup, group.getItemCount());
    }

    /**
     * Get section, given a raw position in the list.
     *
     * @param position
     * @return
     */
    private Group getGroup(int position) {
        int previous = 0;
        int size;
        for (Group group : groups) {
            size = group.getItemCount();
            if (position - previous < size) return group;
            previous += group.getItemCount();
        }
        return null;
    }

    private int getItemCountBeforeGroup(int groupIndex) {
        int count = 0;
        for (Group group : groups.subList(0, groupIndex)) {
            count += group.getItemCount();
        }
        return count;
    }

    public Group getGroup(Item contentItem) {
        for (Group group : groups) {
            if (group.getPosition(contentItem) >= 0) {
                return group;
            }
        }
        return null;
    }

    @Override public void onChanged(Group group) {
        notifyItemRangeChanged(getAdapterPosition(group), group.getItemCount());
    }

    @Override public void onItemInserted(Group group, int position) {
        notifyItemInserted(getAdapterPosition(group) + position);
    }

    @Override public void onItemChanged(Group group, int position) {
        notifyItemChanged(getAdapterPosition(group) + position);
    }

    @Override public void onItemRemoved(Group group, int position) {
        notifyItemRemoved(getAdapterPosition(group) + position);
    }

    @Override public void onItemRangeChanged(Group group, int positionStart, int itemCount) {
        notifyItemRangeChanged(getAdapterPosition(group) + positionStart, itemCount);
    }

    @Override public void onItemRangeInserted(Group group, int positionStart, int itemCount) {
        notifyItemRangeInserted(getAdapterPosition(group) + positionStart, itemCount);
    }

    @Override public void onItemRangeRemoved(Group group, int positionStart, int itemCount) {
        notifyItemRangeRemoved(getAdapterPosition(group) + positionStart, itemCount);
    }

    @Override public void onItemMoved(Group group, int fromPosition, int toPosition) {
        int groupAdapterPosition = getAdapterPosition(group);
        notifyItemMoved(groupAdapterPosition + fromPosition, groupAdapterPosition + toPosition);
    }
}
