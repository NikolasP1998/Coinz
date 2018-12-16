package nikolas.example.com.coinz

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class LeaderboardListAdapter (var mCtx: Context, var resources:Int, var items:List<LeaderboardData>)
    : ArrayAdapter<LeaderboardData>(mCtx,resources,items){
    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        //inflate layout
        val layoutInflater: LayoutInflater = LayoutInflater.from(mCtx)
        val view: View =layoutInflater.inflate(resources,null)

        val textView: TextView =view.findViewById(R.id.tittle)
        val mItems:LeaderboardData=items[position]
        textView.text=mItems.userData
        return view
    }
}