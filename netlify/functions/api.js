import { Writable } from "node:stream";

const TEXT_CONTENT_TYPES = [
  "text/",
  "application/json",
  "application/javascript",
  "application/xml",
  "application/vnd.apple.mpegurl",
  "application/x-mpegurl",
  "image/svg+xml"
];

class BufferingResponse extends Writable {
  constructor() {
    super();
    this.statusCode = 200;
    this.headers = {};
    this.chunks = [];
    this.finishedPromise = new Promise((resolve, reject) => {
      this.once("finish", resolve);
      this.once("error", reject);
    });
  }

  _write(chunk, encoding, callback) {
    this.chunks.push(Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk, encoding));
    callback();
  }

  writeHead(statusCode, headers = {}) {
    this.statusCode = statusCode;
    this.headers = { ...this.headers, ...headers };
    return this;
  }

  setHeader(name, value) {
    this.headers[name] = value;
  }

  getHeader(name) {
    return this.headers[name];
  }

  end(chunk, encoding, callback) {
    if (typeof chunk === "function") {
      callback = chunk;
      chunk = undefined;
      encoding = undefined;
    } else if (typeof encoding === "function") {
      callback = encoding;
      encoding = undefined;
    }

    if (chunk !== undefined) {
      this.chunks.push(Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk, encoding));
    }

    return super.end(callback);
  }

  getBody() {
    return Buffer.concat(this.chunks);
  }
}

function buildFunctionPath(event) {
  const functionPrefix = "/.netlify/functions/api";
  const path = event.path?.startsWith(functionPrefix)
    ? `/api${event.path.slice(functionPrefix.length)}`
    : event.path || "/api";
  const query = event.rawQuery ? `?${event.rawQuery}` : "";
  return `${path}${query}`;
}

function isTextContentType(contentType = "") {
  const normalized = contentType.toLowerCase();
  return TEXT_CONTENT_TYPES.some((candidate) => normalized.startsWith(candidate));
}

export async function handler(event) {
  try {
    const { routeRequest } = await import("../../server.js");
    const req = {
      url: buildFunctionPath(event),
      method: event.httpMethod || "GET",
      headers: {
        host: event.headers?.host || "localhost",
        ...(event.headers || {})
      }
    };

    const res = new BufferingResponse();
    await routeRequest(req, res);
    await res.finishedPromise;

    const body = res.getBody();
    const contentType = res.headers["Content-Type"] || res.headers["content-type"] || "";
    const isText = isTextContentType(contentType);
    const headers = { ...res.headers };
    delete headers["content-length"];
    delete headers["Content-Length"];

    return {
      statusCode: res.statusCode,
      headers,
      body: isText ? body.toString("utf8") : body.toString("base64"),
      isBase64Encoded: !isText
    };
  } catch (error) {
    return {
      statusCode: 500,
      headers: {
        "Content-Type": "application/json; charset=utf-8",
        "Cache-Control": "no-store",
        "Access-Control-Allow-Origin": "*"
      },
      body: JSON.stringify({
        error: error instanceof Error ? error.message : "Function failed"
      })
    };
  }
}
