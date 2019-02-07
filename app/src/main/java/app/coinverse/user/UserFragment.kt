package app.coinverse.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import app.coinverse.Enums.FeedType.DISMISSED
import app.coinverse.Enums.Status.SUCCESS
import app.coinverse.R
import app.coinverse.R.string.*
import app.coinverse.databinding.FragmentUserBinding
import app.coinverse.home.HomeViewModel
import app.coinverse.utils.PROFILE_VIEW
import app.coinverse.utils.SIGN_OUT_ON_BACK_PRESS_DELAY_IN_MILLIS
import com.firebase.ui.auth.AuthUI.getInstance
import com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
import com.google.android.material.snackbar.Snackbar.make
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers.io
import kotlinx.android.synthetic.main.fragment_user.*
import kotlinx.android.synthetic.main.toolbar.*

private val LOG_TAG = UserFragment::class.java.simpleName

class UserFragment : Fragment() {
    private lateinit var analytics: FirebaseAnalytics
    private lateinit var binding: FragmentUserBinding
    private lateinit var userViewModel: UserViewModel
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var user: FirebaseUser
    private lateinit var compositeDisposable: CompositeDisposable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analytics = FirebaseAnalytics.getInstance(FirebaseApp.getInstance()!!.applicationContext)
        analytics.setCurrentScreen(activity!!, PROFILE_VIEW, null)
        userViewModel = ViewModelProviders.of(activity!!).get(UserViewModel::class.java)
        homeViewModel = ViewModelProviders.of(activity!!).get(HomeViewModel::class.java)
        compositeDisposable = CompositeDisposable()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentUserBinding.inflate(inflater, container, false)
        binding.viewmodel = homeViewModel
        user = UserFragmentArgs.fromBundle(arguments!!).user
        binding.user = user
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setToolbar()
        setClickListeners()
    }

    override fun onDestroy() {
        compositeDisposable.dispose()
        super.onDestroy()
    }

    fun setToolbar() {
        toolbar.title = user.displayName
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        (activity as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    fun setClickListeners() {
        dismissedContent.setOnClickListener { view: View ->
            view.findNavController().navigate(R.id.action_userFragment_to_dismissedContentFragment,
                    UserFragmentDirections.actionUserFragmentToDismissedContentFragment().apply {
                        feedType = DISMISSED.name
                    }.arguments)
        }

        signOut.setOnClickListener { view: View ->
            var message: Int
            if (FirebaseAuth.getInstance().currentUser != null) {
                getInstance().signOut(context!!).addOnCompleteListener {
                    if (it.isSuccessful) {
                        homeViewModel.user.value = null
                        message = signed_out
                        make(view, getString(message), LENGTH_SHORT).show()
                        signOut.postDelayed({ activity?.onBackPressed() },
                                SIGN_OUT_ON_BACK_PRESS_DELAY_IN_MILLIS)
                    } //TODO: Add retry.
                }
            } else {
                message = already_signed_out
                make(view, getString(message), LENGTH_SHORT).show()
            }
        }
        delete.setOnClickListener { view: View ->
            if (FirebaseAuth.getInstance().currentUser != null) {
                compositeDisposable.add(userViewModel.deleteUser(user).subscribeOn(io()).observeOn(mainThread())
                        .subscribe { status ->
                            if (status == SUCCESS)
                                getInstance().signOut(context!!).addOnCompleteListener { status ->
                                    if (status.isSuccessful) {
                                        homeViewModel.user.value = null
                                        make(view, getString(deleted), LENGTH_SHORT).show()
                                        delete.postDelayed({
                                            activity?.onBackPressed()
                                        }, SIGN_OUT_ON_BACK_PRESS_DELAY_IN_MILLIS)
                                    }
                                }
                            else make(view, getString(unable_to_delete), LENGTH_SHORT).show()
                        })
            }
        }
    }
}