package nikolas.example.com.coinz

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

class MyListAdapter (var mCtx:Context,var resources:Int,var items:List<Model>)
    :ArrayAdapter<Model>(mCtx,resources,items){
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        //inflate layout
        val layoutInflater:LayoutInflater= LayoutInflater.from(mCtx)
        val view:View=layoutInflater.inflate(resources,null)

        val textView:TextView=view.findViewById(R.id.tittle)
        var mItems:Model=items[position]
        textView.text=mItems.coinData
        return view
    }
}

