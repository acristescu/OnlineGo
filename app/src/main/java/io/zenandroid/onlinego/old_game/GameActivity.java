package io.zenandroid.onlinego.old_game;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import io.zenandroid.onlinego.R;
import io.zenandroid.onlinego.Util;
import io.zenandroid.onlinego.model.Position;
import io.zenandroid.onlinego.model.StoneType;
import io.zenandroid.onlinego.views.BoardView;

/**
 * Created by alex on 05/11/2017.
 */

public class GameActivity extends AppCompatActivity {
    //
    // This is a hardcoded GO game in SGF format: each letter is a coordinate - e.g. a=1, b=2
    // and each pair of letters represents a stone. The color of the stones is given by the order
    // (black always goes first). A "pass" move is represented by "..".
    //
//    String moveSequence = "qdppocdpdddfgccecdqnjpjnfqdqnqpqnogopjomjdmmdjehcncodnbneneognfnfmfohnhoioinhpemeldmcmdldkclblgmckhmgpgkkojoipknkplolpmompnnnpoomjokojrkrjljchnjninklimkmikkkjlkjjfeedkikhjiiijhjgihkghiigijhhjkggghhgfhdhfggedicieidgeffdnrmrormsfpgreqerdresdsfsqgpgphpiqiohogqhqjriqkofrhphngrgqfshpfperfqeoenfmfneodmgseresgrhpklebmalbosdbgbebfaeafbhbdbcadacahaiajbiambkgfhfffhecgbjekfkejglhlflfjdedodlsjsisksfcbccnsndoqdboplqlrmqkrjrhshrisirjsgsegkq";
    String moveSequence = "dpqdqpdcejcjckdjdkeifjfigibkblbjghgkgjekclelfkflglhkhlenikalamakbnepdqcmbmdmcoeqerfresfsdrgqipiqjqhqjrcnbpdogodlgngphofofmfngmiremdnishsjshpjocqbqcrbraoapcsbsegooiihjjhkjinjnlhmijijjmghfpopnqoppqnpmqmroqkqlrlplrnsnrmsprqrpqqpqopnpoqornqmqpksmrknkonnoolnmomnnslsoogpdqcodpemeocndncpcpbmcldlemdkdlcmblbobmanbneqbqenajcjekcecfcedebfdgcgdjdkeieifhchdidcedddeeeeffeffjfjgkfnfoemflfngnhkglgkhkiigliljninjojdgdhcgchbhbiagaicccdbddbbcbbcbcaabbaacfbrbrcscsdsbdafhehfglaihaaaemhmjnlmlokijahbgbfcfhihhhegepsqrprnrsqqs..oipioh";
    Position pos = new Position(19);
    int currentMove = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void nextButtonClicked(View view) {
        if(currentMove >= moveSequence.length() / 2 - 1) {
            return;
        }
        currentMove++;
        StoneType currentPlayer = StoneType.BLACK;
        if(currentMove % 2 != 0) {
            currentPlayer = StoneType.WHITE;
        }
        if(moveSequence.charAt(currentMove * 2) == '.' && moveSequence.charAt(currentMove * 2+1) == '.') {
            Toast.makeText(getApplicationContext(), currentPlayer + " passes", Toast.LENGTH_SHORT).show();
        } else {
            pos.makeMove(currentPlayer, Util.getCoordinatesFromSGF(moveSequence, currentMove * 2));
        }
        ((BoardView) findViewById(R.id.board)).setPosition(pos);
    }

    public void firstButtonClicked(View view) {
        currentMove = -1;
        pos = new Position(19);
        ((BoardView) findViewById(R.id.board)).setPosition(pos);
    }

    public void prevButtonClicked(View view) {
        currentMove --;
        pos = new Position(19);
        StoneType currentPlayer = StoneType.BLACK;
        for(int i = 0; i<=currentMove; i++) {
            currentPlayer = StoneType.BLACK;
            if(i % 2 != 0) {
                currentPlayer = StoneType.WHITE;
            }
            if(!(moveSequence.charAt(i * 2) == '.' && moveSequence.charAt(i * 2+1) == '.')) {
                pos.makeMove(currentPlayer, Util.getCoordinatesFromSGF(moveSequence, i * 2));
            }
        }
        if(moveSequence.charAt(currentMove * 2) == '.' && moveSequence.charAt(currentMove * 2+1) == '.') {
            Toast.makeText(getApplicationContext(), currentPlayer + " passes", Toast.LENGTH_SHORT).show();
        }
        ((BoardView) findViewById(R.id.board)).setPosition(pos);
    }

    public void lastButtonClicked(View view) {
        currentMove = moveSequence.length() / 2 - 1;
        pos = new Position(19);
        StoneType currentPlayer = StoneType.BLACK;
        for(int i = 0; i<=currentMove; i++) {
            currentPlayer = StoneType.BLACK;
            if(i % 2 != 0) {
                currentPlayer = StoneType.WHITE;
            }
            if(!(moveSequence.charAt(i*2) == '.' && moveSequence.charAt(i*2+1) == '.')) {
                pos.makeMove(currentPlayer, Util.getCoordinatesFromSGF(moveSequence, i * 2));
            }
        }
        if(moveSequence.charAt(currentMove * 2) == '.' && moveSequence.charAt(currentMove * 2+1) == '.') {
            Toast.makeText(getApplicationContext(), currentPlayer + " passes", Toast.LENGTH_SHORT).show();
        }
        ((BoardView) findViewById(R.id.board)).setPosition(pos);
    }
}
