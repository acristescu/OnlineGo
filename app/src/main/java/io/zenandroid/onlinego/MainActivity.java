package io.zenandroid.onlinego;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import io.zenandroid.onlinego.login.LoginActivity;
import io.zenandroid.onlinego.model.Position;
import io.zenandroid.onlinego.model.StoneType;
import io.zenandroid.onlinego.ogs.OGSService;
import io.zenandroid.onlinego.views.Board;


public class MainActivity extends AppCompatActivity {

    //
    // This is a hardcoded GO game in SGF format: each letter is a coordinate - e.g. a=1, b=2
    // and each pair of letters represents a stone. The color of the stones is given by the order
    // (black always goes first). A "pass" move is represented by "..".
    // This is the kind of information the webservice would provide (https://online-go.com)
    // but this part is not implemented yet. You can still study my utility methods for
    // interacting with said webservice: BaseAsyncTask and WebServiceRequest
    //
//    String moveSequence = "qdppocdpdddfgccecdqnjpjnfqdqnqpqnogopjomjdmmdjehcncodnbneneognfnfmfohnhoioinhpemeldmcmdldkclblgmckhmgpgkkojoipknkplolpmompnnnpoomjokojrkrjljchnjninklimkmikkkjlkjjfeedkikhjiiijhjgihkghiigijhhjkggghhgfhdhfggedicieidgeffdnrmrormsfpgreqerdresdsfsqgpgphpiqiohogqhqjriqkofrhphngrgqfshpfperfqeoenfmfneodmgseresgrhpklebmalbosdbgbebfaeafbhbdbcadacahaiajbiambkgfhfffhecgbjekfkejglhlflfjdedodlsjsisksfcbccnsndoqdboplqlrmqkrjrhshrisirjsgsegkq";
    String moveSequence = "dpqdqpdcejcjckdjdkeifjfigibkblbjghgkgjekclelfkflglhkhlenikalamakbnepdqcmbmdmcoeqerfresfsdrgqipiqjqhqjrcnbpdogodlgngphofofmfngmiremdnishsjshpjocqbqcrbraoapcsbsegooiihjjhkjinjnlhmijijjmghfpopnqoppqnpmqmroqkqlrlplrnsnrmsprqrpqqpqopnpoqornqmqpksmrknkonnoolnmomnnslsoogpdqcodpemeocndncpcpbmcldlemdkdlcmblbobmanbneqbqenajcjekcecfcedebfdgcgdjdkeieifhchdidcedddeeeeffeffjfjgkfnfoemflfngnhkglgkhkiigliljninjojdgdhcgchbhbiagaicccdbddbbcbbcbcaabbaacfbrbrcscsdsbdafhehfglaihaaaemhmjnlmlokijahbgbfcfhihhhegepsqrprnrsqqs..oipioh";
    Position pos = new Position(19);
    int currentMove = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!OGSService.instance.isLoggedIn()) {
            new Handler().post(() -> startActivity(LoginActivity.Companion.getIntent(this)));
        } else {
            OGSService.instance.registerSeekgraph().subscribe(o -> System.out.println("Got " + o));
            new Handler().postDelayed(OGSService.instance::disconnect, 15000);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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
        ((Board) findViewById(R.id.board)).setPosition(pos);
    }

    public void firstButtonClicked(View view) {
        currentMove = -1;
        pos = new Position(19);
        ((Board) findViewById(R.id.board)).setPosition(pos);
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
        ((Board) findViewById(R.id.board)).setPosition(pos);
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
        ((Board) findViewById(R.id.board)).setPosition(pos);
    }
}
