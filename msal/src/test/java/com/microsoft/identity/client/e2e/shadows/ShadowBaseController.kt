// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.client.e2e.shadows

import com.microsoft.identity.common.java.cache.ICacheRecord
import com.microsoft.identity.common.java.commands.parameters.SilentTokenCommandParameters
import com.microsoft.identity.common.java.controllers.BaseController
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy
import com.microsoft.identity.common.java.providers.oauth2.OAuth2TokenCache
import com.microsoft.identity.common.java.result.AcquireTokenResult
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.shadow.api.Shadow
import org.robolectric.util.ReflectionHelpers.ClassParameter

@Implements(BaseController::class)
class ShadowBaseController {
    companion object {
        private var onRenewAccessTokenInvokedCallback: () -> Unit = {}

        fun setOnRenewAccessTokenInvokedCallback(callback: () -> Unit) {
            onRenewAccessTokenInvokedCallback = callback
        }
    }

    @RealObject
    private lateinit var baseController: BaseController

    @Implementation
    fun renewAccessToken(
        parameters: SilentTokenCommandParameters,
        acquireTokenSilentResult: AcquireTokenResult,
        tokenCache: OAuth2TokenCache<*, *, *>,
        strategy: OAuth2Strategy<*, *, *, *, *, *, *, *, *, *, *, *, *>,
        cacheRecord: ICacheRecord
    ) {
        onRenewAccessTokenInvokedCallback.invoke()

        Shadow.directlyOn<Any, BaseController?>(
            baseController, BaseController::class.java, "renewAccessToken",
            ClassParameter(
                SilentTokenCommandParameters::class.java, parameters
            ),
            ClassParameter(AcquireTokenResult::class.java, acquireTokenSilentResult),
            ClassParameter(
                OAuth2TokenCache::class.java, tokenCache
            ),
            ClassParameter(
                OAuth2Strategy::class.java, strategy
            ),
            ClassParameter(ICacheRecord::class.java, cacheRecord)
        )
    }


}

interface OnRenewAccessTokenInvoked {
    fun alert()
}
