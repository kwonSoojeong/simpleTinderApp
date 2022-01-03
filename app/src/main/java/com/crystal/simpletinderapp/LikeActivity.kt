package com.crystal.simpletinderapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackListener
import com.yuyakaido.android.cardstackview.CardStackView
import com.yuyakaido.android.cardstackview.Direction

class LikeActivity : AppCompatActivity(), CardStackListener {
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var usersDB: DatabaseReference
    private val adapter = CardItemAdapter()
    private val cardItems = mutableListOf<CardItem>()
    private val manager by lazy {
        CardStackLayoutManager(this, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_like)

        usersDB = Firebase.database.reference.child(DBKey.USERS)
        val currentUserDB = usersDB.child(getCurrentUserID())
        //db에 데이터 가져오기
        currentUserDB.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
//                이름이 변경되었을 때, 남이 나를 좋아요했을 때
                if (snapshot.child(DBKey.NAME).value == null) {
                    showNameInputPopup()
                    return
                }
                //유저 정보를 갱신한다
                getUnselectedUsers()
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

        initCardStackView()
        initButtons()
    }

    private fun initButtons() {
        findViewById<Button>(R.id.matchListButton).setOnClickListener {
            startActivity(Intent(this, MatchedUserActivity::class.java))
        }
        findViewById<Button>(R.id.signOutButton).setOnClickListener {
            auth.signOut()
            //Login Page로.
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun initCardStackView() {
        val stackView = findViewById<CardStackView>(R.id.cardStackView)
        stackView.layoutManager = manager
        stackView.adapter = adapter
    }

    private fun showNameInputPopup() {

        //EditText가 포함된 Dialog 띄우기
        val editText = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("이름을 입력해주세요")
            .setView(editText)
            .setPositiveButton("저장") { _, _ ->
                if (editText.text.isEmpty()) {
                    showNameInputPopup()
                } else {
                    saveUserName(editText.text.toString())
                }
            }
            .setCancelable(false)
            .show()
    }

    private fun saveUserName(name: String) {
        val userId = getCurrentUserID()
        //firebase realtime data base 에 저장하기.
        val currentUserDB = usersDB.child(userId)
        val user = mutableMapOf<String, Any>()
        user[DBKey.USER_ID] = userId
        user[DBKey.NAME] = name
        currentUserDB.updateChildren(user)

        //유저정보를 가져온다.
        getUnselectedUsers()
    }

    private fun getUnselectedUsers() {
        usersDB.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if (snapshot.child(DBKey.USER_ID).value != getCurrentUserID()
                    && snapshot.child(DBKey.LIKEDBY).child(DBKey.LIKE).hasChild(getCurrentUserID())
                        .not()
                    && snapshot.child(DBKey.LIKEDBY).child(DBKey.DIS_LIKE)
                        .hasChild(getCurrentUserID()).not()
                ) {
                    //한번도 선택하지 않은 유저
                    val userId = snapshot.child(DBKey.USER_ID).value.toString()
                    var name = "undecided"
                    if (snapshot.child(DBKey.NAME).value != null) {
                        name = snapshot.child(DBKey.NAME).value.toString()
                    }
                    cardItems.add(CardItem(userId, name))
                    adapter.submitList(cardItems)
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                cardItems.find { it.userId == snapshot.key }?.let {
                    it.name = snapshot.child(DBKey.NAME).value.toString()
                }
                adapter.submitList(cardItems)
                adapter.notifyDataSetChanged()
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {

            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    private fun getCurrentUserID(): String {
        if (auth.currentUser == null) {
            Toast.makeText(this, "로그인되어있지 않습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
        return auth.currentUser?.uid.orEmpty()
    }

    override fun onCardDragging(direction: Direction?, ratio: Float) {

    }

    override fun onCardSwiped(direction: Direction?) {
        when (direction) {
            Direction.Right -> like()
            Direction.Left -> disLike()
            else -> {
            }
        }

    }

    private fun disLike() {
        val card = cardItems[manager.topPosition - 1]
        cardItems.removeFirst()
        usersDB.child(card.userId)
            .child(DBKey.LIKEDBY)
            .child(DBKey.DIS_LIKE)
            .child(getCurrentUserID())
            .setValue(true)


        Toast.makeText(this, "${card.name}님을 disLike 하였습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun like() {
        val card = cardItems[manager.topPosition - 1]
        cardItems.removeFirst()
        usersDB.child(card.userId)
            .child(DBKey.LIKEDBY)
            .child(DBKey.LIKE)
            .child(getCurrentUserID())
            .setValue(true)

        //나와 저사람이 매칭이 된 시점을 봐야한다.
        saveMatchIFOtherUserLikeMe(card.userId)

        Toast.makeText(this, "${card.name}님을 Like 하였습니다.", Toast.LENGTH_SHORT).show()

    }

    private fun saveMatchIFOtherUserLikeMe(otherUserID: String) {
        //상대방이 날 좋아한적 있는지 확인
        val otherUserDB =
            usersDB.child(getCurrentUserID()).child(DBKey.LIKEDBY).child(DBKey.LIKE).child(otherUserID)
        otherUserDB.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.value == true) {
                    usersDB.child(getCurrentUserID())
                        .child(DBKey.LIKEDBY)
                        .child(DBKey.MATCH)
                        .child(otherUserID)
                        .setValue(true)
                    usersDB.child(otherUserID)
                        .child(DBKey.LIKEDBY)
                        .child(DBKey.MATCH)
                        .child(getCurrentUserID())
                        .setValue(true)
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }

    override fun onCardRewound() {
    }

    override fun onCardCanceled() {
    }

    override fun onCardAppeared(view: View?, position: Int) {
    }

    override fun onCardDisappeared(view: View?, position: Int) {
    }
}