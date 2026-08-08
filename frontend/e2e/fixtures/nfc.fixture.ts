export const NFC_INIT_SCRIPT = `
(() => {
  class MockNDEFReader extends EventTarget {
    async scan(options) {
      window.__ndefInstance = this;
      if (options && options.signal) {
        var self = this;
        options.signal.addEventListener('abort', function() {
          if (window.__ndefInstance === self) {
            window.__ndefInstance = null;
          }
        }, { once: true });
      }
    }
  }

  window.NDEFReader = MockNDEFReader;

  window.__triggerNFCCard = function(serialNumber) {
    var ndef = window.__ndefInstance;
    if (!ndef) throw new Error('NDEFReader.scan() was not called yet');
    var event = new Event('reading');
    event.serialNumber = serialNumber;
    event.message = { records: [] };
    ndef.dispatchEvent(event);
  };
})();
`

export async function setupNFCMock(page: { addInitScript: (script: string) => Promise<void> }) {
  await page.addInitScript(NFC_INIT_SCRIPT)
}
