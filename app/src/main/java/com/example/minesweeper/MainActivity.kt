package com.example.minesweeper
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random
import kotlinx.coroutines.delay

data class Cell(var isMine:Boolean=false,var isRevealed:Boolean=false,var isFlagged:Boolean=false,var adjacent:Int=0)
enum class Difficulty(val rows:Int,val cols:Int,val mines:Int,val label:String){ EASY(9,9,10,"简单"), MEDIUM(16,16,40,"中等"), HARD(16,30,99,"困难") }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MinesweeperApp() }
    }
}

@Composable
fun MinesweeperApp() {
    var difficulty by remember { mutableStateOf(Difficulty.EASY) }
    var board by remember { mutableStateOf(emptyBoard(difficulty)) }
    var gameOver by remember { mutableStateOf(false) }
    var gameWon by remember { mutableStateOf(false) }
    var firstClick by remember { mutableStateOf(true) }
    var flagMode by remember { mutableStateOf(false) }
    var timer by remember { mutableStateOf(0) }
    var running by remember { mutableStateOf(false) }
    LaunchedEffect(running){ while(running){ delay(1000); timer++ } }

    MaterialTheme(colorScheme = darkColorScheme()){
        Column(Modifier.fillMaxSize().background(Color(0xFF080A12)).padding(16.dp)){
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically){
                Text("扫雷", color=Color.White, fontSize=24.sp, fontWeight=FontWeight.Bold)
                Text("${difficulty.label}", color=Color(0xFF8B95A5))
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween){
                Card(shape=RoundedCornerShape(12.dp), colors=CardDefaults.cardColors(containerColor=Color(0xFF1A1F2E))){ Text(" ⏱ $timer ", Modifier.padding(12.dp), color=Color.White) }
                val flags = board.sumOf { r->r.count{it.isFlagged} }
                Card(shape=RoundedCornerShape(12.dp), colors=CardDefaults.cardColors(containerColor=Color(0xFF1A1F2E))){ Text(" 🚩 ${difficulty.mines-flags} ", Modifier.padding(12.dp), color=Color.White) }
                Button(onClick={ board=emptyBoard(difficulty); gameOver=false; gameWon=false; firstClick=true; timer=0; running=false }){ Text("重开") }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ Difficulty.values().forEach{ d-> FilterChip(selected=difficulty==d, onClick={ difficulty=d; board=emptyBoard(d); gameOver=false; gameWon=false; firstClick=true; timer=0; running=false }, label={Text(d.label)}) } }
            Spacer(Modifier.height(12.dp))
            Column(Modifier.verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState()).align(Alignment.CenterHorizontally)){
                board.forEachIndexed{ r,row->
                    Row{ row.forEachIndexed{ c,cell->
                        Box(Modifier.size(36.dp).padding(2.dp).background(when{ cell.isRevealed-> if(cell.isMine) Color(0xFFFF3B5C) else Color(0xFF1E2535) else->Color(0xFF2A344B)}, RoundedCornerShape(8.dp)).clickable(enabled=!gameOver && !gameWon){
                            if(flagMode){ if(!cell.isRevealed) board=board.mapIndexed{ri,rr-> rr.mapIndexed{ci,cc-> if(ri==r&&ci==c) cc.copy(isFlagged=!cc.isFlagged) else cc}.toMutableList()}.toMutableList()
                            } else {
                                if(firstClick){ board=genBoard(difficulty,r,c); firstClick=false; running=true }
                                if(cell.isFlagged) return@clickable
                                if(cell.isMine){ board=board.map{rr-> rr.map{it.copy(isRevealed=true)}.toMutableList()}.toMutableList(); gameOver=true; running=false }
                                else { board=reveal(board,r,c); if(checkWin(board,difficulty.mines)){ gameWon=true; running=false } }
                            }
                        }, contentAlignment=Alignment.Center){
                            if(cell.isRevealed){ if(cell.isMine) Text("💣") else if(cell.adjacent>0) Text("${cell.adjacent}", color=numberColor(cell.adjacent), fontWeight=FontWeight.Bold) }
                            else if(cell.isFlagged) Text("🚩")
                        }
                    } }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.Center, verticalAlignment=Alignment.CenterVertically){ Switch(checked=flagMode, onCheckedChange={flagMode=it}); Spacer(Modifier.width(8.dp)); Text(if(flagMode)"插旗模式" else "挖掘模式", color=Color.White) }
            if(gameOver) Text("💥 踩雷了！", color=Color(0xFFFF3B5C), fontSize=20.sp, modifier=Modifier.align(Alignment.CenterHorizontally).padding(12.dp))
            if(gameWon) Text("🎉 胜利！用时 ${timer}s", color=Color(0xFF00FFA3), fontSize=20.sp, modifier=Modifier.align(Alignment.CenterHorizontally).padding(12.dp))
        }
    }
}
fun emptyBoard(d:Difficulty)=MutableList(d.rows){ MutableList(d.cols){ Cell() } }
fun genBoard(d:Difficulty,safeR:Int,safeC:Int):MutableList<MutableList<Cell>>{
    val b=emptyBoard(d); var placed=0
    while(placed<d.mines){ val r=Random.nextInt(d.rows); val c=Random.nextInt(d.cols); if((kotlin.math.abs(r-safeR)<=1 && kotlin.math.abs(c-safeC)<=1) || b[r][c].isMine) continue; b[r][c].isMine=true; placed++ }
    for(r in 0 until d.rows) for(c in 0 until d.cols) if(!b[r][c].isMine){ var cnt=0; for(dr in -1..1) for(dc in -1..1){ val nr=r+dr; val nc=c+dc; if(nr in 0 until d.rows && nc in 0 until d.cols && b[nr][nc].isMine) cnt++ }; b[r][c].adjacent=cnt }
    return b
}
fun reveal(board:MutableList<MutableList<Cell>>,r:Int,c:Int):MutableList<MutableList<Cell>>{
    if(r !in board.indices || c !in board[0].indices) return board
    val cell=board[r][c]; if(cell.isRevealed || cell.isFlagged) return board
    board[r][c]=cell.copy(isRevealed=true)
    if(cell.adjacent==0 && !cell.isMine){ for(dr in -1..1) for(dc in -1..1) if(!(dr==0&&dc==0)) reveal(board,r+dr,c+dc) }
    return board
}
fun checkWin(b:List<List<Cell>>,mines:Int)= b.sumOf{row->row.count{it.isRevealed}} == b.size*b[0].size-mines
fun numberColor(n:Int)=when(n){1->Color(0xFF5AC8FA);2->Color(0xFF4CD964);3->Color(0xFFFF3B30);4->Color(0xFF5856D6);5->Color(0xFFFF9500);6->Color(0xFF00D4AA);else->Color.White}
